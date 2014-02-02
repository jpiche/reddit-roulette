package com.jpiche.redditroulette.fragments

import android.preference.PreferenceFragment
import android.os.Bundle
import com.jpiche.redditroulette._
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.SharedPreferences
import android.util.Log

final case class SettingsFragment() extends PreferenceFragment with BaseFrag with OnSharedPreferenceChangeListener {

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    getPreferenceManager.setSharedPreferencesName(Prefs.PREF_NAME)
    addPreferencesFromResource(R.xml.preferences)
  }

  override def onResume() {
    super.onResume()
    getPreferenceManager.getSharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  override def onPause() {
    super.onPause()
    getPreferenceManager.getSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
  }

  override def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    debug("pref <%s> changed" format key)
    return
  }
}

object SettingsFragment extends FragTag