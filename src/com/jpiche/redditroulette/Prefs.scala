package com.jpiche.redditroulette

import android.content.{Context, SharedPreferences}

final class Prefs(val shared: SharedPreferences) {
  def allowNsfw = shared.getBoolean(Prefs.PREF_NSFW, false)
  def showSelf = shared.getBoolean(Prefs.PREF_SELF, false)
  def swipeLeftNext = shared.getBoolean(Prefs.PREF_SWIPE_LEFT_NEXT, true)
}

object Prefs {
  final val PREF_NAME = "redditroulette"
  final val PREF_NSFW = "allow_nsfw"
  final val PREF_SELF = "show_self"
  final val PREF_SWIPE_LEFT_NEXT = "swipe_left_next"

  def apply(context: Context) = new Prefs(context.getSharedPreferences(PREF_NAME, 0))
  implicit def sharedFromPrefs(prefs: Prefs) = prefs.shared
}