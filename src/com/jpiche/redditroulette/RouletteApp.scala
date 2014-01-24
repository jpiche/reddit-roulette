package com.jpiche.redditroulette

import android.app.Application
import android.util.Log

final class RouletteApp extends Application with LogTag {

  override def onCreate() {
    super.onCreate()


  }

  override def onLowMemory() {
    super.onLowMemory()

    Log.w(LOG_TAG, "onLowMemory!")
    return
  }
}

private[redditroulette] object RouletteApp {

  final val REDDIT_CLIENTID = "IoH3IRvEt-GY6w"
  final val REDDIT_SECRET = "8haRIqbLB476V13HCfcjdSZr3EY"

  final val REDDIT_REDIRECT = "http://redditroulette.jpiche.com/app-redirect/"
  final val REDDIT_SCOPE = "read,save"

  final val USER_AGENT = "redditroulette/1.0 by jpiche"
  final val CHECKPOINT_PLAY = "CHECKPOINT_PLAY"
}
