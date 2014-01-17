package com.jpiche.redditroulette.activities

import scalaz._, Scalaz._
import scalaz.std.boolean.unless

import scala.concurrent.ExecutionContext.Implicits.global
import android.os.{Message, Handler, Bundle}
import android.util.Log
import android.app.{Activity, Fragment}
import com.jpiche.redditroulette.reddit.{Thing, Subreddit}
import android.view.{WindowManager, MenuItem, View}
import com.jpiche.redditroulette.fragments._
import android.app.FragmentManager.OnBackStackChangedListener
import com.testflightapp.lib.TestFlight
import android.content.{Context, Intent}
import android.net.ConnectivityManager
import com.jpiche.redditroulette._
import scala.util.{Failure, Success}
import com.jpiche.redditroulette.net.WebData
import java.util.concurrent.atomic.AtomicBoolean
import org.joda.time.DateTime


final class MainActivity extends Activity with BaseAct with TypedViewHolder {

  private lazy val progressLayout = findView(TR.progressLayout)
  private lazy val progress = findView(TR.progress)

  private val isLoading = new AtomicBoolean(false)

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
      next(pop = false)
    }
  }.some

  private val backStackListener = new OnBackStackChangedListener {
    def onBackStackChanged() = shouldActionUp()
  }


  private sealed abstract class AbstractThingListener extends ThingListener {
    def onNext() {
      next(pop = true)
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
  }.some

  private val webListener = new AbstractThingListener {
    def onError(thing: Option[Thing]) {
      manager.popBackStack()
      toast(R.string.url_load_error)
    }
  }.some


  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    getWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    setContentView(R.layout.main)

    if (savedInstanceState == null) {
      val homeFrag = HomeFragment(homeListener)
      val t = manager.beginTransaction()
      t.add(R.id.container, homeFrag, HomeFragment.FRAG_TAG)
      t.commit()

      unless (prefs contains Prefs.PREF_NSFW) {
        NsfwDialogFragment().show(manager, NsfwDialogFragment.FRAG_TAG)
      }
    }

    progress.setMax(100)

    manager addOnBackStackChangedListener backStackListener
    shouldActionUp()
  }

  override def onAttachFragment(frag: Fragment) {
    frag match {
      case h: HomeFragment => h.listener = homeListener
      case w: WebFragment => w.listener = webListener
      case i: ImageFragment => i.listener = imageListener
      case _ => return
    }
  }

  override def onNavigateUp(): Boolean = {
    manager.popBackStack()
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
        Log.i(LOG_TAG, "settings menu item")
        val i = new Intent(this, classOf[SettingsActivity])
        startActivity(i)
        true

      // TODO: Finish login code so that this can work
      /*
      case R.id.login =>
        Log.i(LOG_TAG, "login clicked")
        val i = new Intent(this, classOf[LoginActivity])
        startActivity(i)
        true

      case R.id.logout =>
        Log.i(LOG_TAG, "logout clicked")
        true
      */

      case _ => super.onOptionsItemSelected(item)
    }

  private def shouldActionUp() {
    getActionBar.setDisplayHomeAsUpEnabled(manager.getBackStackEntryCount > 0)
  }

  // make it a lazy val so that it only gets sent once per instance
  private lazy val checkpoint =
    TestFlight.passCheckpoint(RouletteApp.CHECKPOINT_PLAY)

  private def next(pop: Boolean) {
    if (isLoading.get()) {
      Log.i(LOG_TAG, "Ran next() while post was still loading")
      return
    }
    isLoading.set(true)

    unless(hasConnection) {
      toast(R.string.no_internet)
      return
    }
    checkpoint

    progressHandler.sendEmptyMessage(View.VISIBLE)

    Subreddit.random onComplete {
      case Success(sub) =>
        sub.next onComplete {
          case Success((web: WebData, thing: Thing)) =>
            Log.i(LOG_TAG, s"success: ${web.url}; thing: $thing")

            db.findThingVisited(thing.id) match {
              case Some(time: Long) if DateTime.now.getMillis.toDouble - time < 60 =>
                Log.w(LOG_TAG, s"skipping! with time $time and diff ${DateTime.now.getMillis.toDouble - time}")
                next(pop)

              case _ =>
                db add thing

                val (frag, tag) = if (thing.isImg || web.isImage)
                  (ImageFragment(imageListener, thing), ImageFragment.FRAG_TAG)
                else
                  (WebFragment(webListener, thing), WebFragment.FRAG_TAG)

                runOnUiThread(new Runnable {
                  def run() {
                    if (pop) {
                      manager.popBackStack()
                    }
                    addFrag(frag, tag)
                  }
                })
            }

          case Failure(e) => nextFail(e)
        }
      case Failure(e) => nextFail(e)
    }

    return
  }

  private def nextFail(e: Throwable) {
    Log.e(LOG_TAG, "api exception: %s" format e)
    toast(R.string.api_load_error)
    progressHandler.sendEmptyMessage(View.GONE)

    val home = manager.findFragmentByTag(HomeFragment.FRAG_TAG).asInstanceOf[HomeFragment]
    if (home != null) {
      home.showBtn()
    }
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
