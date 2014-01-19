package com.jpiche.redditroulette

import android.content.Context
import android.os.{Build, Message, Handler}
import android.os.Handler.Callback
import android.widget.Toast
import android.app.{FragmentManager, Activity, Fragment}
import android.view.{View, Window, WindowManager}
import com.jpiche.redditroulette.net.WebSettings

sealed trait Base extends LogTag {

  // abstract; needs to be implemented for `Activity` and `Fragment` separately
  protected val thisContext: Context

  protected def manager: FragmentManager
  protected implicit lazy val db: Db = Db(thisContext)

  private lazy val toastHandler = new Handler(thisContext.getMainLooper, new Callback {
    def handleMessage(msg: Message): Boolean = {
      if (msg.obj != null && msg.obj.isInstanceOf[String])
          Toast.makeText(thisContext, msg.obj.asInstanceOf[String], Toast.LENGTH_LONG).show()
      else
          Toast.makeText(thisContext, msg.what, Toast.LENGTH_LONG).show()
      false
    }
  })

  protected def toast(text: String) {
    Message.obtain(toastHandler, 0, text).sendToTarget()
  }

  protected def toast(text: Int) {
    toastHandler.sendEmptyMessage(text)
    return
  }

  protected implicit def prefs = Prefs(thisContext)
  protected implicit lazy val webSettings = WebSettings(RouletteApp.USER_AGENT)
}

trait BaseAct extends Base { this: Activity =>
  protected implicit val thisContext = this
  protected lazy val manager = getFragmentManager
}

trait BaseFrag extends Base { this: Fragment =>
  // must be lazy and not called until after fragment has been attached
  // to an activity
  protected implicit lazy val thisContext = getActivity
  protected lazy val manager = getFragmentManager
}

trait LogTag {
  protected val LOG_TAG = getClass.getSimpleName
}

trait FragTag {
  val FRAG_TAG = getClass.getName
}
