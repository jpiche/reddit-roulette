package com.jpiche.redditroulette.activities

import android.os.Bundle
import com.jpiche.redditroulette.fragments.SettingsFragment
import android.app.Activity
import com.jpiche.redditroulette.BaseAct

final class SettingsActivity extends Activity with BaseAct {

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    if (inst == null) {
      val s = SettingsFragment()
      val t = manager.beginTransaction()
      t.add(android.R.id.content, s, SettingsFragment.FRAG_TAG)
      t.commit()
    }

    getActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onNavigateUp(): Boolean = {
    finish()
    true
  }
}
