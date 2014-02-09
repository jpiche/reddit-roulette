package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.jpiche.redditroulette.RouletteApp
import android.util.Log
import com.jpiche.hermes.{HermesFail, HermesSuccess, Hermes, HermesSettings}


final case class AccessToken(
  accessToken: String,
  tokenType: String,
  refreshToken: String,
  scope: String
)

object AccessToken {
  private val accessTokenUrl = "https://ssl.reddit.com/api/v1/access_token"

  def request(code: String, state: String)(implicit webSettings: HermesSettings): Future[Option[AccessToken]] = {
    val params = Map(
      "state" -> state,
      "scope" -> RouletteApp.REDDIT_SCOPE,
      "client_id" -> RouletteApp.REDDIT_CLIENTID,
      "redirect_uri" -> RouletteApp.REDDIT_REDIRECT,
      "code" -> code,
      "grant_type" -> "authorization_code",
      "response_type" -> "code"
    )

    val auth = (RouletteApp.REDDIT_CLIENTID, RouletteApp.REDDIT_SECRET)

    Hermes.post(accessTokenUrl, params, auth) collect {
      case web: HermesSuccess =>
        Log.d("AccessToken", s"access token response: ${web.asString}")
        web.asString.decodeOption[AccessToken]

      case fail: HermesFail =>
        Log.d("AccessToken", s"access token response (code ${fail.status}): $fail")
        None
    }
  }

  def refresh(refresh: String, state: String)(implicit webSettings: HermesSettings): Future[Option[AccessToken]] = {
    val params = Map(
      "state" -> state,
      "scope" -> RouletteApp.REDDIT_SCOPE,
      "client_id" -> RouletteApp.REDDIT_CLIENTID,
      "redirect_uri" -> RouletteApp.REDDIT_REDIRECT,
      "grant_type" -> "refresh_token",
      "response_type" -> "code",
      "refresh_token" -> refresh
    )

    val auth = (RouletteApp.REDDIT_CLIENTID, RouletteApp.REDDIT_SECRET)

    Hermes.post(accessTokenUrl, params, auth) collect {
      case web: HermesSuccess =>
        Log.d("AccessToken", s"refresh token response: ${web.asString}")
        web.asString.decodeOption[AccessToken]

      case fail: HermesFail =>
        Log.d("AccessToken", s"access token response (code ${fail.status}): $fail")
        None
    }
  }

  implicit def AccessTokenCodecJson: CodecJson[AccessToken] =
    casecodec4(AccessToken.apply, AccessToken.unapply)("access_token", "token_type", "refresh_token", "scope")
}
