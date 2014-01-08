package com.jpiche.redditroulette.activities

import scalaz._, Scalaz._
import scalaz.std.boolean.unless

import scala.concurrent.ExecutionContext.Implicits.global
import android.os.{Message, Handler, Bundle}
import android.util.Log
import android.app.{Activity, Fragment}
import com.jpiche.redditroulette.reddit.{Thing, Subreddit}
import android.view.{WindowManager, MenuItem, View}
import com.jpiche.redditroulette.fragments.{NsfwDialogFragment, WebFragment, ImageFragment, HomeFragment}
import android.app.FragmentManager.OnBackStackChangedListener
import com.testflightapp.lib.TestFlight
import android.content.{Context, Intent}
import android.net.ConnectivityManager
import com.jpiche.redditroulette._
import scala.util.Failure
import scala.Some
import scala.util.Success

final class MainActivity extends Activity with BaseAct with TypedViewHolder {

  private lazy val progress = findView(TR.progress)

  private val progressHander = new Handler(new Handler.Callback {
    def handleMessage(msg: Message): Boolean = {
      msg.obj match {
        case "percent" => progress setProgress msg.what
        case _ => progress setVisibility msg.what
      }
      false
    }
  })

  private lazy val homeListener = new HomeFragment.Listener {
    override def clickedGo() {
      loadItem()
    }
  }.some

  private lazy val imageListener = new ImageFragment.Listener {
    def onError(thing: Option[Thing]) {
      manager.popBackStack()
      thing match {
        case Some(t) =>
          Log.w(LOG_TAG, "Retrying URL with WebFragment: %s" format t.url)
          runOnUiThread(new Runnable {
            def run() = addFrag(WebFragment(webListener, t), WebFragment.FRAG_TAG)
          })

        case None =>
          toast(R.string.url_load_error)
      }
    }
    def onFinished() {
      progressHander.sendEmptyMessage(View.GONE)
      return
    }
  }.some

  private lazy val webListener = new WebFragment.Listener {
    def onError() {
      manager.popBackStack()
      toast(R.string.url_load_error)
    }
    def onFinished() {
      progressHander.sendEmptyMessage(View.GONE)
      return
    }
    def onProgress(prog: Int) {
      Message.obtain(progressHander, prog, "percent").sendToTarget()
    }
  }.some

  private lazy val backStackListener = new OnBackStackChangedListener {
    def onBackStackChanged() = shouldActionUp()
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    getWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

    setContentView(R.layout.main)

    if (savedInstanceState == null) {
      val homeFrag = HomeFragment(homeListener)
      val t = manager.beginTransaction()
      t.add(TR.container.id, homeFrag, HomeFragment.FRAG_TAG)
      t.commit()

      unless (prefs contains RouletteApp.PREF_NSFW) {
        NsfwDialogFragment().show(manager, NsfwDialogFragment.FRAG_TAG)
      }
    }

    progress.setMax(100)

    manager addOnBackStackChangedListener backStackListener
    shouldActionUp()
  }

  override def onAttachFragment(frag: Fragment) {
    frag match {
      case h@HomeFragment() => h.listener = homeListener
      case w@WebFragment() => w.listener = webListener
      case i@ImageFragment() => i.listener = imageListener
      case _ => return
    }
  }

  override def onNavigateUp(): Boolean = {
    manager.popBackStack()
    true
  }

  override def onBackPressed() {
    // TODO: double check this code
    if (manager.getBackStackEntryCount > 0) {
      val tag = manager.getBackStackEntryAt(manager.getBackStackEntryCount - 1).getName
      val f = manager findFragmentByTag tag

      if (f == null)
        manager.popBackStack()
      else f match {
        case web@WebFragment() if web.webView.canGoBack => web.webView.goBack()
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

      case R.id.about =>
        Log.i(LOG_TAG, "about menu item")
        true

      case R.id.login =>
        Log.i(LOG_TAG, "login clicked")
        val i = new Intent(this, classOf[LoginActivity])
        startActivity(i)
        true

      case _ => super.onOptionsItemSelected(item)
    }

  private def shouldActionUp() {
    getActionBar.setDisplayHomeAsUpEnabled(manager.getBackStackEntryCount > 0)
  }

  private def loadItem() {
    Log.d(LOG_TAG, "loadItem")
    if ( ! hasConnection) {
      toast(R.string.no_internet)
      return
    }
    TestFlight.passCheckpoint(RouletteApp.CHECKPOINT_PLAY)

    progressHander.sendEmptyMessage(View.VISIBLE)

    Subreddit.random map {
      _.next andThen {
        case _ =>
          progressHander.sendEmptyMessage(View.GONE)

      } onComplete {
        case Success(thing: Thing) if thing.isImg =>
          runOnUiThread(new Runnable {
            def run() = addFrag(ImageFragment(imageListener, thing), ImageFragment.FRAG_TAG)
          })

        case Success(thing: Thing) =>
          runOnUiThread(new Runnable {
            def run() = addFrag(WebFragment(webListener, thing), WebFragment.FRAG_TAG)
          })

        case Failure(e) =>
          Log.e(LOG_TAG, "api exception: %s" format e)
          toast(R.string.api_load_error)

          val home = manager.findFragmentByTag(HomeFragment.FRAG_TAG).asInstanceOf[HomeFragment]
          if (home != null) {
            home.showBtn()
          }
      }
    }

    return
  }

  private def addFrag(frag: Fragment, tag: String) {
    val t = manager.beginTransaction()
    t.replace(TR.container.id, frag, tag)
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
