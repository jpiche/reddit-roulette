package com.jpiche.redditroulette.fragments

import android.preference.PreferenceFragment
import android.os.Bundle
import com.jpiche.redditroulette.R


class SettingsFragment extends PreferenceFragment {

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    addPreferencesFromResource(R.xml.preferences)
  }
}
