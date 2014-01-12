package com.jpiche.redditroulette.activities

import scalaz._, Scalaz._

import android.app.{Fragment, Activity}
import com.jpiche.redditroulette.{RouletteApp, BaseAct}
import android.os.Bundle
import com.jpiche.redditroulette.fragments.LoginFragment
import android.util.Log
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.jpiche.redditroulette.fragments.LoginFragment.Listener

final class LoginActivity extends Activity with BaseAct {

  private lazy val authUrl: Uri = "https://ssl.reddit.com/api/v1/authorize.compact"
  private lazy val params = Seq(
    "client_id" -> RouletteApp.REDDIT_CLIENTID,
    "redirect_uri" -> RouletteApp.REDDIT_REDIRECT,
    "scope" -> RouletteApp.REDDIT_SCOPE,
    "duration" -> "permanent",
    "state" -> "asdfasdf",
    "response_type" -> "code"
  )

  private lazy val loginUrl: String = {
    val loginUri = authUrl addParams params
    loginUri.toString()
  }

  private lazy val loginListener = new Listener {
    def onLoginRedirect(code: String, state: String) {

    }
  }

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    if (inst == null) {
      Log.d(LOG_TAG, s"loginUrl: $loginUrl")
      val s = LoginFragment(loginUrl)
      val t = manager.beginTransaction()
      t.add(android.R.id.content, s, LoginFragment.FRAG_TAG)
      t.commit()
    }

    getActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onAttachFragment(frag: Fragment) {
    frag match {
      case login@LoginFragment() => login.listener = loginListener.some
      case _ => return
    }
  }

  override def onNavigateUp(): Boolean = {
    finish()
    true
  }
}
