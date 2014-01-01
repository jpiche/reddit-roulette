package com.jpiche.redditroulette.fragments

import android.preference.PreferenceFragment
import android.os.Bundle
import com.jpiche.redditroulette.{RouletteApp, R}

final class SettingsFragment extends PreferenceFragment {

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    getPreferenceManager.setSharedPreferencesName(RouletteApp.PREF_NAME)
    addPreferencesFromResource(R.xml.preferences)
  }
}
