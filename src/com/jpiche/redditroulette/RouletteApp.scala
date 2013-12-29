package com.jpiche.redditroulette

import android.app.Application
import com.testflightapp.lib.TestFlight

class RouletteApp extends Application {

  override def onCreate() {
    super.onCreate()

    TestFlight.takeOff(this, "cfbfca8d-e88f-4ea7-856a-2a77339d2a9e")
  }
}

object RouletteApp {
  val CHECKPOINT_PLAY = "CHECKPOINT_PLAY"
}
