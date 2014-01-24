package com.jpiche.redditroulette.activities

import scalaz._, Scalaz._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, promise}
import scala.util.{Failure, Success}
import scala.collection.mutable

import android.os.Bundle
import android.util.Log
import android.app.{Activity, Fragment}
import android.view.MenuItem
import android.content.{Context, Intent}
import android.net.ConnectivityManager

import android.support.v4.view.PagerAdapter.POSITION_NONE
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener
import android.support.v13.app.FragmentStatePagerAdapter

import com.jpiche.redditroulette.reddit.Thing
import com.jpiche.redditroulette.fragments._
import com.jpiche.redditroulette._
import com.jpiche.redditroulette.net.{Web, WebFail, WebData}

import com.google.analytics.tracking.android.EasyTracker
import org.joda.time.DateTime
import com.jpiche.redditroulette.views.ZoomOutPageTransformer
import java.io.{FileOutputStream, File}


final class MainActivity extends Activity with BaseAct with TypedViewHolder {

  private sealed trait ThingData {
    val webData: WebData
    val thing: Thing
  }
  private final case class ThingWebData(webData: WebData, thing: Thing) extends ThingData
  private final case class ThingBitmapData(webData: WebData, thing: Thing) extends ThingData

  private lazy val cacheDir = {
    val ex = getExternalCacheDir
    val cache = new File(ex, "img")
    cache.mkdirs()
    cache
  }

  private lazy val viewPagerAdapter = new FragmentStatePagerAdapter(getFragmentManager) with ThingPagerAdapter
  private lazy val viewPager = findView(TR.viewPager)

  private var lastSub: Option[String] = None
  private var allowNsfwPref = false

  private val frags = mutable.ArrayBuffer.empty[Fragment]
  private var lastClear: Long = DateTime.now.getMillis


  private val homeListener = new HomeFragment.Listener {
    override def clickedGo() {
      clearFrags()
      run {
        val size = frags.size
        viewPager.setCurrentItem(size - 1, true)
      }
    }
  }

  private val nsfwListener = new NsfwDialogListener {
    override def onDismiss() {
      next(1)
      return
    }
  }

  private val viewPagerListener = new SimpleOnPageChangeListener {
    override def onPageSelected(p: Int) {

      val count = frags.count {
        case load: LoadingFragment => true
        case _ => false
      }

      Log.d(LOG_TAG, s"onPageSelected ($p) with loading count ($count); frags(p): ${frags(p)}")

      val size = frags.size
      if (p > 0 && count <= 2 && p == size - 1) {
        run {
          frags += LoadingFragment(size - 1)
          viewPagerAdapter.notifyDataSetChanged()
          next(size - 1)
        }
      } else if (count > 2 && p == size - 1) {
        // too many, wait
      }

      frags(p) match {
        case frag: ThingFragment if frag.thing.isDefined =>
          getActionBar setTitle frag.thing.get.title
        case load: LoadingFragment =>
          getActionBar setTitle R.string.loading
        case home: HomeFragment =>
          getActionBar setTitle R.string.app_name
      }

      shouldActionUp()
    }
  }

  private sealed abstract class AbstractThingListener extends ThingListener {
    def onNext(position: Int) {
      val i = viewPager.getCurrentItem
      if (i < frags.size) {
        viewPager.setCurrentItem(i + 1, true)
      } else {
        viewPager.setCurrentItem(i - 1)
        viewPager.setCurrentItem(i)
      }
    }

    def saveThing(thing: Thing) {
      import com.netaporter.uri.dsl._
      if (prefs.isLoggedIn) {
        val uri = "https://ssl.reddit.com/api/save" ? ("access_token" -> prefs.accessToken) & ("id" -> thing.name)
        Web.post(uri.toString()) onComplete {
          case Success(web@WebData(_, _, _)) =>
            Log.i(LOG_TAG, s"https://ssl.reddit.com/api/save worked! ${web.toString}")
          case Success(WebFail(conn)) =>
            Log.e(LOG_TAG, s"https://ssl.reddit.com/api/save error: ${conn.getContent}")
          case Failure(e) =>
            Log.e(LOG_TAG, s"https://ssl.reddit.com/api/save error: $e")
        }
      } else {
        toast("Not logged in. Try logging in first.")
      }
      return
    }
  }

  private val imageListener = new AbstractThingListener {
    def onError(position: Int, thing: Option[Thing]) {
      thing match {
        case Some(t) =>
          Log.w(LOG_TAG, "Retrying URL with WebFragment: %s" format t.url)
          replaceFrag(position, WebFragment(position, t))

        case None =>
          Log.w(LOG_TAG, "Thing is empty")
          toast(R.string.url_load_error)
      }
    }
  }

  private val webListener = new AbstractThingListener {
    def onError(position: Int, thing: Option[Thing]) {
      toast(R.string.url_load_error)
      next(position)
    }
  }


  override def onStart() {
    super.onStart()

    EasyTracker.getInstance(this).activityStart(this)
  }

  override def onStop() {
    super.onStop()

    EasyTracker.getInstance(this).activityStop(this)
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    val start = if (savedInstanceState == null) {
      if (prefs contains Prefs.PREF_NSFW) {
        true
      } else {
        NsfwDialogFragment().show(manager, NsfwDialogFragment.FRAG_TAG)
        false
      }
    } else {
      true
    }

    allowNsfwPref = prefs.allowNsfw

    frags ++= Seq(HomeFragment(), LoadingFragment(1))

    if (start) {
      // On first launch, the NSFW dialog will show, in which case, we don't
      // want to start pre-fetching until a NSFW pref is chosen.
      next(1)
    }

    viewPager setAdapter viewPagerAdapter
    viewPager setOnPageChangeListener viewPagerListener
    viewPager setOffscreenPageLimit 1 // this is the default, but let's be explicit about it
    viewPager setPageTransformer(true, ZoomOutPageTransformer())
  }

  override def onResume() {
    super.onResume()

    val newNsfw = prefs.allowNsfw
    if (newNsfw != allowNsfwPref) {
      allowNsfwPref = newNsfw
    }

    // reset time so any pending futures get zombied, forcing all prefs to be re-read
    lastClear = DateTime.now.getMillis
    val s = frags.size - 1
    if (viewPager.getCurrentItem == s) {
      next(s)
    }

    db.subCache = None

    shouldActionUp()
    return
  }

  override def onAttachFragment(frag: Fragment) {
    frag match {
      case h: HomeFragment => h.listener = homeListener.some
      case w: WebFragment => w.listener = webListener.some
      case i: ImageFragment => i.listener = imageListener.some
      case n: NsfwDialogFragment => n.listener = nsfwListener.some
      case _ => return
    }
  }

  private def clearFrags() {
    if (frags.size > 1) {
      run {
        lastClear = DateTime.now.getMillis
        frags.reduceToSize(1)
        frags += LoadingFragment(1)
        viewPagerAdapter.notifyDataSetChanged()
      }
      next(1)
    }
  }

  override def onNavigateUp(): Boolean = {
    clearFrags()
    run {
      viewPager.setCurrentItem(0)
    }
    true
  }

  override def onBackPressed() {
    val p = viewPager.getCurrentItem
    if (p > 0)
      frags(p) match {
        case web@WebFragment() if web.webView.canGoBack =>
          web.webView.goBack()
        case _ =>
          viewPager.setCurrentItem(p - 1, true)
      }
    else
      super.onBackPressed()
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    item.getItemId match {
      case R.id.settings =>
        val i = new Intent(this, classOf[SettingsActivity])
        startActivity(i)
        true

      // TODO: Finish login code so that this can work
      case R.id.login =>
        Log.i(LOG_TAG, "login clicked")
        val i = new Intent(this, classOf[LoginActivity])
        startActivity(i)
        true

      case R.id.logout =>
        Log.i(LOG_TAG, "logout clicked")
        true

      case _ => super.onOptionsItemSelected(item)
    }

  private def shouldActionUp() {
    getActionBar.setDisplayHomeAsUpEnabled(viewPager.getCurrentItem > 0)
  }

  /**
   * This is an aggregate function for subreddit and "thing" finding which
   * returns a composite `Future[ThingData]` at the end of the chain, which
   * is a simple sealed collection of case classes for `(WebData, Thing)`.
   */
  private def nextThing: Future[ThingData] = {
    val p = promise[ThingData]()

    val sub = db.nextSub(prefs.allowNsfw, lastSub)
    sub.next onComplete {
      case Success((web: WebData, thing: Thing)) =>

        // if the post is marked nsfw even though the subreddit is not,
        // and the user has nsfw off, skip it
        if (thing.over18 && ! prefs.allowNsfw) {
          p completeWith nextThing

        // look at the db for distance between this and that last time it was shown
        } else if (needsSkip(thing)) {
          p completeWith nextThing

        // else, it should be safe
        } else {
          val fallback = ThingWebData(web, thing)
          if (thing.isImg && ! thing.likelyGif) {
            Web.get(thing.goodUrl) onComplete {
              case Success(webBmp@WebData(_, data, Some(hash))) if ! web.contentType.isGif =>

                val imgFile = new File(cacheDir, hash)
                if (imgFile.canWrite) {
                  Log.d(LOG_TAG, s"can write to file path: ${imgFile.getAbsolutePath}")
                  val writer = new FileOutputStream(imgFile)
                  writer.write(data, 0, data.length)
                  writer.flush()
                  writer.close()
                }

                p success ThingBitmapData(webBmp, thing)

              case Success(_) =>
                p success fallback

              case Failure(e) =>
                Log.e(LOG_TAG, s"api exception loading url (${thing.goodUrl}: $e")
                p success fallback
            }
          } else {
            p success fallback
          }
          lastSub = sub.name.some
        }

      case Failure(e) =>
        Log.e(LOG_TAG, s"api exception: $e")
        p failure e
    }
    p.future
  }

  /**
   * Function for checking the database for whether the thing passed in needs
   * to be skipped or not.
   *
   * @param thing The thing to check whether we need to skip it and load a
   *              new one.
   * @return      `true` indicates that the thing needs to be skipped.
   */
  private def needsSkip(thing: Thing): Boolean = {
    try {
      if (db.shouldSkipThing(thing.id, 40)) {
        Log.w(LOG_TAG, s"skipping!! thing: $thing")
        true
      } else {
        db add thing
        false
      }
    } catch {
      case e: IllegalStateException =>
        Log.w(LOG_TAG, s"IllegalStateException when calling findThingVisited: $e")
        false
    }
  }

  private def replaceFrag(p: Int, f: Fragment) {
    if (p >= 0 && p < frags.size) {
      run {
        frags.update(p, f)
        viewPagerAdapter.notifyDataSetChanged()
      }
    }
  }

  /**
   * This function takes the `thingQueue` and actually assigns the things to
   * fragments.
   */
  private def next(p: Int) {
    if ( ! hasConnection) {
      toast(R.string.no_internet)
      return
    }

    val f = nextThing
    val time = DateTime.now.getMillis

    def x(f: Fragment, thing: Thing) {
      // If cleared after this fragment was requested, just drop it
      if (lastClear > time) return

      replaceFrag(p, f)
      if (viewPager.getCurrentItem == p) {
        run {
          getActionBar setTitle thing.title
        }
      }
    }

    f onComplete {
      case Success(ThingBitmapData(webData: WebData, thing: Thing)) =>
//        Log.d(LOG_TAG, s"Success with ThingBitmapData: $thing")
        val f = ImageFragment(p, thing, webData)
        x(f, thing)

      case Success(ThingWebData(_, thing: Thing)) =>
//        Log.d(LOG_TAG, s"Success with ThingWebData: $thing")
        val f = WebFragment(p, thing)
        x(f, thing)

      case Failure(e) =>
        Log.i(LOG_TAG, s"Failure in next(): $e")
    }
  }

  private def hasConnection: Boolean = {
    val conn = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val net = conn.getActiveNetworkInfo
    net != null && net.isConnected
  }

  // ------

  private trait ThingPagerAdapter extends FragmentStatePagerAdapter with LogTag {

    override def getCount: Int = frags.size
    override def getItem(p: Int): Fragment = frags(p)

    override def getItemPosition(obj: Any): Int = {
      obj match {
        case f: Fragment if frags contains f =>
          frags indexOf f
        case _ => POSITION_NONE
      }
    }
  }
}
