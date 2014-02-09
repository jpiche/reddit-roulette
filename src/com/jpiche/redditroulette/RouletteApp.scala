package com.jpiche.redditroulette

import android.app.Application
import android.content.pm.ApplicationInfo
import com.testflightapp.lib.TestFlight

final class RouletteApp extends Application {

  private lazy val isDebuggable = 0 != (getBaseContext.getApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE)

  override def onCreate() {
    if (isDebuggable) {
      TestFlight.takeOff(this, RouletteApp.TESTFLIGHT_TOKEN)
    }
    super.onCreate()
  }
}

private[redditroulette] object RouletteApp {

  final val TESTFLIGHT_TOKEN = "cfbfca8d-e88f-4ea7-856a-2a77339d2a9e"

  final val REDDIT_CLIENTID = "IoH3IRvEt-GY6w"
  final val REDDIT_SECRET = "8haRIqbLB476V13HCfcjdSZr3EY"

  final val REDDIT_REDIRECT = "http://redditroulette.jpiche.com/app-redirect/"
  final val REDDIT_SCOPE = "read,save"

  final val USER_AGENT = "redditroulette/1.0 by jpiche"
  final val CHECKPOINT_PLAY = "CHECKPOINT_PLAY"

  final val STORE_URL = "market://details?id=com.jpiche.redditroulette"
}
