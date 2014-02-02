package com.jpiche.redditroulette.activities

import scalaz._, Scalaz._

import android.app.{Fragment, Activity}
import com.jpiche.redditroulette._
import android.os.Bundle
import com.jpiche.redditroulette.fragments.LoginFragment
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.jpiche.redditroulette.fragments.LoginFragment.Listener
import com.google.analytics.tracking.android.EasyTracker
import com.jpiche.redditroulette.reddit.AccessToken
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import android.view.View


final class LoginActivity extends Activity with BaseAct with TypedViewHolder {

  private val authUrl: Uri = "https://ssl.reddit.com/api/v1/authorize.compact"
  private val params = Seq(
    "client_id" -> RouletteApp.REDDIT_CLIENTID,
    "redirect_uri" -> RouletteApp.REDDIT_REDIRECT,
    "scope" -> RouletteApp.REDDIT_SCOPE,
    "duration" -> "permanent",
    "state" -> "bvbhjiuyt454rf",
    "response_type" -> "code"
  )

  private val loginUrl: String = {
    val loginUri = authUrl addParams params
    loginUri.toString()
  }

  private lazy val progressLayout = findView(TR.progressLayout)

  private val loginListener = new Listener {
    def onLoginRedirect(code: String, state: String) {
      run {
        progressLayout setVisibility View.VISIBLE
      }

      AccessToken.request(code, state) onComplete {
        case Success(Some(AccessToken(access, _, refresh, _))) =>
          prefs accessToken access
          prefs refreshToken refresh

          toast("Login successful!")
          finish()

        case _ =>
          toast("Error while authenticating. Please try again.")

          val s = LoginFragment(loginUrl)
          run {
            progressLayout setVisibility View.GONE

            val t = manager.beginTransaction()
            t.replace(android.R.id.content, s, LoginFragment.FRAG_TAG)
            t.commit()
          }
      }
    }
  }

  override def onStart() {
    super.onStart()

    EasyTracker.getInstance(this).activityStart(this)
  }

  override def onStop() {
    super.onStop()

    EasyTracker.getInstance(this).activityStop(this)
  }

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)
    setContentView(R.layout.login)

    if (inst == null) {
      debug(s"loginUrl: $loginUrl")
      val s = LoginFragment(loginUrl)
      val t = manager.beginTransaction()
      t.add(R.id.container, s, LoginFragment.FRAG_TAG)
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
