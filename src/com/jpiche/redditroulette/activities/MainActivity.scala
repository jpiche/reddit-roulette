package com.jpiche.redditroulette.activities

import scalaz._, Scalaz._
import scalaz.std.boolean.unless

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

import com.google.analytics.tracking.android.EasyTracker
import org.joda.time.DateTime
import com.jpiche.redditroulette.views.ZoomOutPageTransformer
import java.io.{FileOutputStream, File}
import com.jpiche.hermes.{HermesRequest, HermesFail, Hermes, HermesSuccess}
import com.squareup.okhttp.internal.Base64
import android.widget.Toast


final class MainActivity extends Activity with BaseAct with TypedViewHolder {

  private sealed trait ThingData {
    val webData: HermesSuccess
    val thing: Thing
  }
  private final case class ThingWebData(webData: HermesSuccess, thing: Thing) extends ThingData
  private final case class ThingBitmapData(webData: HermesSuccess, thing: Thing) extends ThingData

//  private lazy val cacheDir = {
//    val ex = getExternalCacheDir
//    val cache = new File(ex, "img")
//    cache.mkdirs()
//    cache
//  }

  private lazy val viewPagerAdapter = new FragmentStatePagerAdapter(getFragmentManager) with ThingPagerAdapter
  private lazy val viewPager = findView(TR.viewPager)

  private var lastSub: Option[String] = None
  private var allowNsfwPref = false

  private val frags = mutable.ArrayBuffer.empty[Fragment]
  private var lastClear: Long = DateTime.now.getMillis
  private val futures = mutable.HashMap.empty[Int, Future[ThingData]]


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
      allowNsfwPref = prefs.allowNsfw
    }
  }

  private val viewPagerListener = new SimpleOnPageChangeListener {
    override def onPageSelected(p: Int) {

      val count = frags.count {
        case load: LoadingFragment => true
        case _ => false
      }

//      Log.d(LOG_TAG, s"onPageSelected ($p) with loading count ($count); frags(p): ${frags(p)}")

      if (p > 0
        && p == frags.size - 1
        && futures.get(p).isEmpty
      ) {
        if (count <= 2) {
          run {
            frags += LoadingFragment(p)
            viewPagerAdapter.notifyDataSetChanged()
            next(p)
          }
        } else {
          toast(R.string.load_too_many)
        }
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
      if (prefs.isLoggedIn) {
        val url = "https://oauth.reddit.com/api/save"
        val params = Map(
          "id" -> thing.name,
          "grant_type" -> "authorization_code"
        )
        val get = HermesRequest.post(url.toString, params)
        val req = get.addHeader(("Authorization", s"Bearer ${prefs.accessToken}"))

        Hermes.http(req) onComplete {
          case Success(web@HermesSuccess(_, _)) =>
            debug(s"$url worked! (status ${web.status}): ${web.asString}")
            toast("Post saved", Toast.LENGTH_SHORT)
          case Success(fail@HermesFail(conn)) =>
            warn(s"$url error (status ${fail.status}): ${conn.getContent}")
          case Failure(e) =>
            warn(s"$url error: $e")
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
          warn("Retrying URL with WebFragment: %s" format t.url)
          replaceFrag(position, WebFragment(position, t))

        case None =>
          warn("Thing is empty")
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

    unless (prefs contains Prefs.PREF_NSFW) {
      NsfwDialogFragment().show(manager, NsfwDialogFragment.FRAG_TAG)
    }

    allowNsfwPref = prefs.allowNsfw

    frags ++= Seq(HomeFragment(), LoadingFragment(1))

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

    if (prefs.allowNsfw != allowNsfwPref
      || prefs.lastSubredditUpdate > lastClear
    ) {
      lastClear = DateTime.now.getMillis
      clearFrags()
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
      futures.clear()
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
        debug("login clicked")
        val i = new Intent(this, classOf[LoginActivity])
        startActivity(i)
        true

      case R.id.logout =>
        debug("logout clicked")
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
  private def nextThing(position: Int): Future[ThingData] = {
    val p = promise[ThingData]()

    val sub = db.nextSub(allowNsfwPref, lastSub)
    frags(position) match {
      case loading: LoadingFragment =>
        loading.setLoadingText(R.string.finding_next_post)
      case _ =>
    }
    sub.next onComplete {
      case Success((web: HermesSuccess, thing: Thing)) =>

        // if the post is marked nsfw even though the subreddit is not,
        // and the user has nsfw off, skip it
        if (thing.over18 && ! prefs.allowNsfw) {
          p completeWith nextThing(position)

        // look at the db for distance between this and that last time it was shown
        } else if (needsSkip(thing)) {
          p completeWith nextThing(position)

        // else, it should be safe
        } else {
          val fallback = ThingWebData(web, thing)
          if (thing.isImg && ! thing.likelyGif) {
            val loading = frags(position) match {
              case loading: LoadingFragment =>
                loading.setLoadingText(R.string.downloading_image)
                loading.some
              case _ => none
            }
            Hermes.get(thing.goodUrl, x => {
              val p = Math.round(x * 100)
              loading.map { _.setProgress(p) }
            }) onComplete {
              case Success(webBmp@HermesSuccess(_, data)) if ! web.contentType.isGif =>

//                val imgFile = new File(cacheDir, thing.url64)
//                if (imgFile.canWrite) {
//                  Log.d(LOG_TAG, s"can write to file path: ${imgFile.getAbsolutePath}")
//                  val writer = new FileOutputStream(imgFile)
//                  writer.write(data, 0, data.length)
//                  writer.flush()
//                  writer.close()
//                }

                p success ThingBitmapData(webBmp, thing)

              case Success(_) =>
                p success fallback

              case Failure(e) =>
                warn(s"api exception loading url (${thing.goodUrl}: $e")
                p success fallback
            }
          } else {
            p success fallback
          }
          lastSub = sub.name.some
        }

      case Failure(e) =>
        warn(s"api exception: $e")
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
        warn(s"skipping!! thing: $thing")
        true
      } else {
        db add thing
        false
      }
    } catch {
      case e: IllegalStateException =>
        warn(s"IllegalStateException when calling findThingVisited: $e")
        false
    }
  }

  private def replaceFrag(p: Int, f: Fragment) {
    if (p >= 0
      && p < frags.size
    ) {
      run {
        try {
          frags.update(p, f)
          viewPagerAdapter.notifyDataSetChanged()
        } catch {
          case e: IllegalStateException =>
            // ignore it and move on
        }
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

    val ff = futures get p
    if (ff.isDefined && ! ff.get.isCompleted) {
      return
    }

    val fut = nextThing(p)
    futures.put(p, fut)
    val time = DateTime.now.getMillis

    def x(f: Fragment, thing: Thing) {
      // If cleared after this fragment was requested, just drop it
      if (lastClear > time
        || futures.get(p).isEmpty
        || futures.get(p).get != fut
      ) return

      replaceFrag(p, f)
      if (viewPager.getCurrentItem == p) {
        run {
          getActionBar setTitle thing.title
        }
      }
    }

    fut onComplete {
      case Success(ThingBitmapData(webData, thing)) =>
        val f = ImageFragment(p, thing, webData)
        x(f, thing)
        futures.remove(p)

      case Success(ThingWebData(_, thing)) =>
        val f = WebFragment(p, thing)
        x(f, thing)
        futures.remove(p)

      case Failure(e) =>
        futures.remove(p)
        debug(s"Failure in next(): $e")
    }
  }

  private def hasConnection: Boolean = {
    val conn = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val net = conn.getActiveNetworkInfo
    net != null && net.isConnected
  }

  // ------

  private trait ThingPagerAdapter extends FragmentStatePagerAdapter {

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
