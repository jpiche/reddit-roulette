package com.jpiche.redditroulette

import android.app.Application
import com.testflightapp.lib.TestFlight

final class RouletteApp extends Application {

  override def onCreate() {
    super.onCreate()

    TestFlight.takeOff(this, RouletteApp.TESTFLIGHT_KEY)
  }
}

object RouletteApp {
  private val TESTFLIGHT_KEY = "cfbfca8d-e88f-4ea7-856a-2a77339d2a9e"

  // this is just for the fun of it
  private[redditroulette] val REDDIT_CLIENTID =
    x"190292280813490094467369871628242069532367012201670483823459349" ^ "vkO0Y@#$%xSrJMzOz$(@)UmSJb"

  val REDDIT_REDIRECT = "http://redditroulette.jpiche.com/app-redirect/"
  val REDDIT_REDIRECT_HOST = "redditroulette.jpiche.com"
  val REDDIT_SCOPE = "read,save"

  val USER_AGENT = "redditroulette/1.0 by jpiche"
  val CHECKPOINT_PLAY = "CHECKPOINT_PLAY"

  val PREF_NAME = "redditroulette"
  val PREF_NSFW = "allow_nsfw"
  val PREF_SELF = "show_self"
}
