package com.jpiche.redditroulette

import android.app.Application
import com.testflightapp.lib.TestFlight
import android.util.Log

final class RouletteApp extends Application with LogTag {

  override def onCreate() {
    super.onCreate()

//    TestFlight.takeOff(this, RouletteApp.TESTFLIGHT_KEY)
  }

  override def onLowMemory() {
    super.onLowMemory()

    Log.w(LOG_TAG, "onLowMemory!")
    return
  }
}

object RouletteApp {
//  private final val TESTFLIGHT_KEY = "cfbfca8d-e88f-4ea7-856a-2a77339d2a9e"

  // this is just for the fun of it
  private[redditroulette] val REDDIT_CLIENTID =
    x"190292280813490094467369871628242069532367012201670483823459349" ^ "vkO0Y@#$%xSrJMzOz$(@)UmSJb"

  final val REDDIT_REDIRECT = "http://redditroulette.jpiche.com/app-redirect/"
  final val REDDIT_REDIRECT_HOST = "redditroulette.jpiche.com"
  final val REDDIT_SCOPE = "read,save"

  final val USER_AGENT = "redditroulette/1.0 by jpiche"
  final val CHECKPOINT_PLAY = "CHECKPOINT_PLAY"
}
