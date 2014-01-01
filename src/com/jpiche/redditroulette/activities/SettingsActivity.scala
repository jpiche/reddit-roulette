package com.jpiche.redditroulette.activities

import android.os.Bundle
import com.jpiche.redditroulette.fragments.SettingsFragment
import com.jpiche.redditroulette.Base
import android.app.Activity

final class SettingsActivity extends Activity with Base {
  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    if (inst == null) {
      val s = new SettingsFragment
      getFragmentManager.beginTransaction().add(android.R.id.content, s).commit()
    }
    getActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onNavigateUp(): Boolean = {
    finish()
    true
  }
}
