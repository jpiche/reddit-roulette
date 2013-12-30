package com.jpiche.redditroulette.activities

import android.os.Bundle
import com.jpiche.redditroulette.fragments.SettingsFragment


class SettingsActivity extends BaseActivity {
  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val s = new SettingsFragment
    manager.beginTransaction().replace(android.R.id.content, s).commit()
    getActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onNavigateUp(): Boolean = {
    finish()
    true
  }
}
