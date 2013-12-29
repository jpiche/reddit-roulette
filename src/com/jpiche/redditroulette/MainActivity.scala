package com.jpiche.redditroulette

import scalaz._, Scalaz._

import scala.concurrent.ExecutionContext.Implicits.global
import android.os.{Message, Handler, Bundle}
import android.util.Log
import android.widget.Toast
import android.app.{Fragment, Activity}
import com.jpiche.redditroulette.reddit.{Thing, Subreddit}
import scala.util.{Failure, Success}
import android.view.View
import android.os.Handler.Callback
import com.jpiche.redditroulette.fragments.{WebFragment, ImageFragment, HomeFragment}

class MainActivity extends Activity with TypedViewHolder {

  private lazy val LOG_TAG = this.getClass.getSimpleName
  private lazy val manager = getFragmentManager
  private lazy val progress = findView(TR.progress)
  private val toastHandler = new Handler(new Callback {
    def handleMessage(msg: Message): Boolean = {
      Toast.makeText(getApplicationContext, msg.what, Toast.LENGTH_LONG).show()
      false
    }
  })
  private val progressHander = new Handler(new Handler.Callback {
    def handleMessage(msg: Message): Boolean = {
      progress.setVisibility(msg.what)
      false
    }
  })

  private lazy val homeListener = new HomeFragment.Listener {
    override def clickedGo() {
      loadItem()
    }
  }.some

  private lazy val imageListener = new ImageFragment.Listener {
    def onError() {
      manager.popBackStack()
      toast(R.string.url_load_error)
    }
  }.some

  private lazy val webListener = new WebFragment.Listener {
    def onError() {
      manager.popBackStack()
      toast(R.string.url_load_error)
    }
  }.some

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    if (savedInstanceState == null || manager.getBackStackEntryCount == 0) {
      val homeFrag = HomeFragment(homeListener)
      val t = manager.beginTransaction()
      t.replace(TR.container.id, homeFrag, HomeFragment.FRAG_TAG)
      t.commit()
    } else {
      Range.apply(0, manager.getBackStackEntryCount) foreach { i =>
        val tag = manager.getBackStackEntryAt(i).getName
        val f = manager findFragmentByTag tag
        if (f == null) return
        f match {
          case h@HomeFragment() => h.listener = homeListener
          case w@WebFragment() => w.listener = webListener
          case i@ImageFragment() => i.listener = imageListener
        }
      }
    }

    return
  }

  override def onBackPressed() {
    if (manager.getBackStackEntryCount > 0) {
      val tag = manager.getBackStackEntryAt(manager.getBackStackEntryCount - 1).getName
      val f = manager findFragmentByTag tag
      if (f == null) manager.popBackStack()
      else f match {
        case web@WebFragment() if web.webView.canGoBack => web.webView.goBack()
        case _ => manager.popBackStack()
      }
    } else {
      super.onBackPressed()
    }
  }

  private def loadItem() {
    Log.d(LOG_TAG, "loadImage")

    progressHander.sendEmptyMessage(View.VISIBLE)

    Subreddit.random.next andThen {
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

  private def toast(text: Int) {
    toastHandler.sendEmptyMessage(text)
    return
  }
}
