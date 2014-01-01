package com.jpiche.redditroulette

import android.content.Context
import android.os.{Message, Handler}
import android.os.Handler.Callback
import android.widget.Toast
import android.app.Fragment


trait Base { self =>
  protected lazy val thisContext: Context = self match {
    case x if x.isInstanceOf[Context] => x.asInstanceOf[Context]
    case x if x.isInstanceOf[Fragment] => x.asInstanceOf[Fragment].getActivity
    case _ => throw new ClassCastException("Trait Base isn't an instance of context or fragment? what's going on here...")
  }

  protected lazy val LOG_TAG = self.getClass.getSimpleName
  protected implicit lazy val db = Db(thisContext)

  private val toastHandler = new Handler(new Callback {
    def handleMessage(msg: Message): Boolean = {
      Toast.makeText(thisContext, msg.what, Toast.LENGTH_LONG).show()
      false
    }
  })

  protected def toast(text: Int) {
    toastHandler.sendEmptyMessage(text)
    return
  }

  protected def prefs = thisContext.getSharedPreferences(RouletteApp.PREF_NAME, 0)
}
