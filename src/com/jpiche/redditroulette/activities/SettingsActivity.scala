package com.jpiche.redditroulette.activities

import android.os.Bundle
import com.jpiche.redditroulette.fragments.SettingsFragment

final class SettingsActivity extends BaseActivity {
  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    if (inst == null) {
      val s = new SettingsFragment
      manager.beginTransaction().add(android.R.id.content, s).commit()
    }
    getActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onNavigateUp(): Boolean = {
    finish()
    true
  }
}
