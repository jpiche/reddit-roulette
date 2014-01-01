package com.jpiche.redditroulette.activities

import android.app.Activity
import com.jpiche.redditroulette.{RouletteApp, Db}


abstract class BaseActivity extends Activity { self =>
  protected lazy val LOG_TAG = self.getClass.getSimpleName
  protected lazy val manager = self.getFragmentManager

  implicit lazy val db = Db(self)

  def prefs = getSharedPreferences(RouletteApp.PREF_NAME, 0)
}
