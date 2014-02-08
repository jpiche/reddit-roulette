package com.jpiche.redditroulette

import android.content.{Context, SharedPreferences}
import org.joda.time.DateTime

final class Prefs(val shared: SharedPreferences) {
  import Prefs.{apply => _, sharedFromPrefs => _, _}

  def allowNsfw = shared.getBoolean(PREF_NSFW, false)
  def showSelf = shared.getBoolean(PREF_SELF, false)
  def lastUpdate = shared.getLong(PREF_LAST_UPDATE, DateTime.now.getMillis)
  def didUpdate() {
    shared.edit().putLong(PREF_LAST_UPDATE, DateTime.now.getMillis).commit()
    ()
  }

  def accessToken = shared.getString(REDDIT_ACCESS_TOKEN, "")
  def accessToken(token: String) = shared.edit().putString(REDDIT_ACCESS_TOKEN, token).commit()

  def refreshToken = shared.getString(REDDIT_REFRESH_TOKEN, "")
  def refreshToken(token: String) = shared.edit().putString(REDDIT_REFRESH_TOKEN, token).commit()

  def isLoggedIn = accessToken != ""
  def logout() {
    accessToken("")
    refreshToken("")
    ()
  }
}

object Prefs {
  final val PREF_NAME = "redditroulette"
  final val PREF_NSFW = "allow_nsfw"
  final val PREF_SELF = "show_self"
  final val PREF_LAST_UPDATE = "last_update"

  final val REDDIT_ACCESS_TOKEN = "reddit_access_token"
  final val REDDIT_REFRESH_TOKEN = "reddit_refresh_token"

  def apply(context: Context) = new Prefs(context.getSharedPreferences(PREF_NAME, 0))
  implicit def sharedFromPrefs(prefs: Prefs) = prefs.shared
}