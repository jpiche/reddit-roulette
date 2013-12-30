package com.jpiche.redditroulette.activities

import android.app.Activity


abstract class BaseActivity extends Activity {
  protected lazy val LOG_TAG = this.getClass.getSimpleName
  protected lazy val manager = getFragmentManager
}
