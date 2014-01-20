package com.jpiche.redditroulette

import android.content.{Context, SharedPreferences}

final class Prefs(val shared: SharedPreferences) {
  def allowNsfw = shared.getBoolean(Prefs.PREF_NSFW, false)
  def showSelf = shared.getBoolean(Prefs.PREF_SELF, false)
  def swipeLeftNext = shared.getBoolean(Prefs.PREF_SWIPE_LEFT_NEXT, true)

  def accessToken = shared.getString(Prefs.REDDIT_ACCESS_TOKEN, "")
  def accessToken(token: String) = shared.edit().putString(Prefs.REDDIT_ACCESS_TOKEN, token).commit()

  def refreshToken = shared.getString(Prefs.REDDIT_REFRESH_TOKEN, "")
  def refreshToken(token: String) = shared.edit().putString(Prefs.REDDIT_REFRESH_TOKEN, token).commit()

  def isLoggedIn = accessToken == ""
}

object Prefs {
  final val PREF_NAME = "redditroulette"
  final val PREF_NSFW = "allow_nsfw"
  final val PREF_SELF = "show_self"
  final val PREF_SWIPE_LEFT_NEXT = "swipe_left_next"

  final val REDDIT_ACCESS_TOKEN = "reddit_access_token"
  final val REDDIT_REFRESH_TOKEN = "reddit_refresh_token"

  def apply(context: Context) = new Prefs(context.getSharedPreferences(PREF_NAME, 0))
  implicit def sharedFromPrefs(prefs: Prefs) = prefs.shared
}