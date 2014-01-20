package com.jpiche.redditroulette.activities

import scalaz._, Scalaz._
import scalaz.std.boolean.unless

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, promise}
import scala.util.{Failure, Success}
import scala.collection.mutable

import java.util.concurrent.atomic.AtomicBoolean

import android.os.{Message, Handler, Bundle}
import android.util.Log
import android.app.{FragmentManager, Activity, Fragment}
import android.view.{WindowManager, MenuItem, View}
import android.app.FragmentManager.OnBackStackChangedListener
import android.content.{Context, Intent}
import android.net.ConnectivityManager

import com.jpiche.redditroulette.reddit.Thing
import com.jpiche.redditroulette.fragments._
import com.jpiche.redditroulette._
import com.jpiche.redditroulette.net.{Web, WebFail, WebData}

import com.google.analytics.tracking.android.EasyTracker


final class MainActivity extends Activity with BaseAct with TypedViewHolder {

  private sealed trait ThingData {
    val webData: WebData
    val thing: Thing
  }
  private final case class ThingWebData(webData: WebData, thing: Thing) extends ThingData
  private final case class ThingBitmapData(webData: WebData, thing: Thing) extends ThingData


  private lazy val progressLayout = findView(TR.progressLayout)
  private lazy val progress = findView(TR.progress)

  private var lastSub: Option[String] = None
  private var allowNsfwPref = false

  private val isLoading = new AtomicBoolean(false)
  private val thingQueue = new mutable.SynchronizedQueue[Future[ThingData]]
  private val handler = new Handler()

  private val progressHandler = new Handler(new Handler.Callback {
    def handleMessage(msg: Message): Boolean = {
      msg.obj match {
        case "percent" => progress setProgress msg.what
        case _ => progressLayout setVisibility msg.what
      }
      false
    }
  })

  private val homeListener = new HomeFragment.Listener {
    override def clickedGo() {
      next()
    }
  }

  private val nsfwListener = new NsfwDialogListener {
    override def onDismiss() {
      thingQueue += nextThing
      return
    }
  }

  private val backStackListener = new OnBackStackChangedListener {
    def onBackStackChanged() = shouldActionUp()
  }


  private sealed abstract class AbstractThingListener extends ThingListener {
    def onNext() {
      next()
    }

    def onPrev() {
      manager.popBackStack()
    }

    def onFinished() {
      progressHandler.sendEmptyMessage(View.GONE)
      isLoading.set(false)
      return
    }

    def onProgress(prog: Int) {
      Message.obtain(progressHandler, prog, "percent").sendToTarget()
    }
  }

  private val imageListener = new AbstractThingListener {
    def onError(thing: Option[Thing]) {
      manager.popBackStack()
      thing match {
        case Some(t) =>
          Log.w(LOG_TAG, "Retrying URL with WebFragment: %s" format t.url)
          runOnUiThread(new Runnable {
            def run() = addFrag(WebFragment(webListener, t), WebFragment.FRAG_TAG)
          })

        case None =>
          Log.w(LOG_TAG, "Thing is empty")
          toast(R.string.url_load_error)
      }
    }
  }

  private val webListener = new AbstractThingListener {
    def onError(thing: Option[Thing]) {
      manager.popBackStack()
      toast(R.string.url_load_error)
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

    // This was here for testing and screenshots only.
    // getWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    setContentView(R.layout.main)

    val start = if (savedInstanceState == null) {
      val homeFrag = HomeFragment(homeListener.some)
      val t = manager.beginTransaction()
      t.add(R.id.container, homeFrag, HomeFragment.FRAG_TAG)
      t.commit()

      if (prefs contains Prefs.PREF_NSFW) {
        true
      } else {
        NsfwDialogFragment(nsfwListener).show(manager, NsfwDialogFragment.FRAG_TAG)
        false
      }
    } else {
      true
    }

    progress.setMax(100)
    allowNsfwPref = prefs.allowNsfw

    if (start) {
      // On first launch, the NSFW dialog will show, in which case, we don't
      // want to start pre-fetching until a NSFW pref is chosen.
      thingQueue += nextThing
    }

    manager addOnBackStackChangedListener backStackListener
    shouldActionUp()
  }

  override def onResume() {
    super.onResume()

    val newNsfw = prefs.allowNsfw
    if (newNsfw != allowNsfwPref) {
      allowNsfwPref = newNsfw

      // TODO: kill the threads first
      thingQueue.clear()

      thingQueue += nextThing
    }
    return
  }

  override def onAttachFragment(frag: Fragment) {
    frag match {
      case h: HomeFragment => h.listener = homeListener.some
      case w: WebFragment => w.listener = webListener.some
      case i: ImageFragment => i.listener = imageListener.some
      case _ => return
    }
  }

  override def onNavigateUp(): Boolean = {
    manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    isLoading.set(false)
    progressHandler.sendEmptyMessage(View.GONE)
    true
  }

  override def onBackPressed() {
    // TODO: double check this code
    if (manager.getBackStackEntryCount > 0) {
      val tag = manager.getBackStackEntryAt(manager.getBackStackEntryCount - 1).getName
      val f = manager findFragmentByTag tag

      progressHandler.sendEmptyMessage(View.GONE)
      isLoading.set(false)

      if (f == null)
        manager.popBackStack()
      else f match {
        case web: WebFragment if web.webView.canGoBack => web.webView.goBack()
        case _ => manager.popBackStack()
      }
    } else {
      super.onBackPressed()
    }
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
    getActionBar.setDisplayHomeAsUpEnabled(manager.getBackStackEntryCount > 0)
  }

  /**
   * This is an aggregate function for subreddit and "thing" finding which
   * returns a composite `Future[ThingData]` at the end of the chain, which
   * is a simple sealed collection of case classes for `(WebData, Thing)`.
   */
  private def nextThing: Future[ThingData] = {
    val p = promise[ThingData]()

    db.nextSub(prefs.allowNsfw, lastSub) onComplete {
      case Success(Some(sub)) =>
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
              if ((thing.isImg && ! thing.goodUrl.toLowerCase.endsWith("gif")) || web.isImage) {
                Web.get(thing.goodUrl) onComplete {
                  case Success(webBmp: WebData) if webBmp.contentType.toLowerCase != "image/gif" =>
                    p success ThingBitmapData(webBmp, thing)

                  case Success(fail: WebFail) =>
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
      case Success(None) =>
        Log.e(LOG_TAG, s"Failed to find the next subreddit?")
        p completeWith nextThing

      case Failure(e) =>
        Log.e(LOG_TAG, s"Failed to load next subreddit. Error: $e")
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
      if (db.shouldSkipThing(thing.id, 30)) {
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

  /**
   * This function takes the `thingQueue` and actually assigns the things to
   * fragments.
   */
  private def next() {
    if (isLoading.get()) {
      Log.i(LOG_TAG, "Ran next() while post was still loading")
      return
    }
    isLoading.set(true)

    unless(hasConnection) {
      toast(R.string.no_internet)
      return
    }

    progressHandler.sendEmptyMessage(View.VISIBLE)
    val f = if (thingQueue.size > 0)
      thingQueue.dequeue()
    else
      nextThing

    thingQueue += nextThing

    def add(frag: Fragment, tag: String) =
      handler.post(new Runnable {
        override def run() {
          addFrag(frag, tag)
        }
      })

    f onComplete {
      case Success(ThingBitmapData(webData: WebData, thing: Thing)) =>
        Log.i(LOG_TAG, s"Success with ThingWebData: $thing")
        val f = ImageFragment(imageListener, thing, webData)
        add(f, ImageFragment.FRAG_TAG)

      case Success(ThingWebData(webData: WebData, thing: Thing)) =>
        Log.i(LOG_TAG, s"Success with ThingBitmapData: $thing")
        val f = WebFragment(webListener, thing)
        add(f, WebFragment.FRAG_TAG)

      case Failure(e) =>
        Log.i(LOG_TAG, s"Failure in next(): $e")
        progressHandler.sendEmptyMessage(View.GONE)
    }

    return
  }

  private def addFrag(frag: Fragment, tag: String) {
    val t = manager.beginTransaction()
    t.replace(R.id.container, frag, tag)
    t.addToBackStack(null)
    t.commit()
    return
  }

  private def hasConnection: Boolean = {
    val conn = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    val net = conn.getActiveNetworkInfo
    net != null && net.isConnected
  }
}
