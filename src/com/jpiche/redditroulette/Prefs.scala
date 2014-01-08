package com.jpiche.redditroulette

import android.content.{Context, SharedPreferences}

final class Prefs(val shared: SharedPreferences) {
  def allowNsfw = shared.getBoolean(RouletteApp.PREF_NSFW, false)
  def showSelf = shared.getBoolean(RouletteApp.PREF_SELF, false)
}

object Prefs {
  def apply(context: Context) = new Prefs(context.getSharedPreferences(RouletteApp.PREF_NAME, 0))
  implicit def sharedFromPrefs(prefs: Prefs) = prefs.shared
}