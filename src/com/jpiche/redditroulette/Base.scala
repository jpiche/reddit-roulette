package com.jpiche.redditroulette

import android.content.Context
import android.os.{Looper, Handler}
import android.widget.Toast
import android.app.{FragmentManager, Activity, Fragment}
import com.jpiche.hermes.HermesSettings
import android.util.Log
import android.content.pm.ApplicationInfo


sealed trait Base { self =>

  protected val LOG_TAG = self.getClass.getSimpleName

  // abstract; needs to be implemented for `Activity` and `Fragment` separately
  protected val thisContext: Context
  protected implicit val thisHandler: Handler

  protected def manager: FragmentManager
  protected implicit lazy val db: Db = Db(thisContext)


  protected def toast(text: String): Unit = toast(text, Toast.LENGTH_LONG)
  protected def toast(text: String, duration: Int) {
    run {
      Toast.makeText(thisContext, text, duration).show()
    }
  }

  protected def toast(text: Int): Unit = toast(text, Toast.LENGTH_LONG)
  protected def toast(text: Int, duration: Int) {
    run {
      Toast.makeText(thisContext, text, duration).show()
    }
  }

  protected implicit def prefs = Prefs(thisContext)
  protected implicit lazy val webSettings = HermesSettings(RouletteApp.USER_AGENT)

  protected def run(f: => Any)(implicit handler: Handler): Unit = {
    handler.post(new Runnable {
      def run() {
        f
        return
      }
    })
    return
  }

  private lazy val isDebuggable = 0 != (thisContext.getApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE)

  protected def debug(msg: => String) {
    if (isDebuggable)
      Log.d(LOG_TAG, msg)
    return
  }

  protected def warn(msg: => String) {
    if (isDebuggable)
      Log.w(LOG_TAG, msg)
    return
  }
}

trait BaseAct extends Base { this: Activity =>
  protected implicit val thisContext = this
  protected lazy val manager = getFragmentManager

  protected implicit val thisHandler = new Handler()
}

trait BaseFrag extends Base { this: Fragment =>
  // must be lazy and not called until after fragment has been attached
  // to an activity
  protected implicit lazy val thisContext = getActivity
  protected lazy val manager = getFragmentManager

  protected implicit val thisHandler = new Handler(Looper.getMainLooper)
}


trait FragTag {
  val FRAG_TAG = getClass.getName
}
