package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.jpiche.redditroulette.RouletteApp
import com.jpiche.redditroulette.net.{WebSettings, WebFail, WebData, Web}
import com.netaporter.uri.dsl._

final case class AccessToken(
  accessToken: String,
  tokenType: String,
  refreshToken: String,
  scope: String
)

object AccessToken {
  private val accessTokenUrl = "https://ssl.reddit.com/api/v1/access_token"

  def request(code: String, state: String)(implicit webSettings: WebSettings): Future[Option[AccessToken]] = {
    val params = Seq(
      "state" -> state,
      "scope" -> RouletteApp.REDDIT_SCOPE,
      "client_id" -> RouletteApp.REDDIT_CLIENTID,
      "redirect_uri" -> RouletteApp.REDDIT_REDIRECT,
      "code" -> code,
      "grant_type" -> "authorization_code",
      "response_type" -> "code"
    )
    val uri = accessTokenUrl addParams params
    uri.withUser(RouletteApp.REDDIT_CLIENTID)
    uri.withPassword(RouletteApp.REDDIT_SECRET)

    Web.post(uri.toString()) collect {
      case web: WebData =>
        web.asString.decodeOption[AccessToken]

      case fail: WebFail =>
        None
    }
  }

  implicit def AccessTokenCodecJson: CodecJson[AccessToken] =
    casecodec4(AccessToken.apply, AccessToken.unapply)("access_token", "token_type", "refresh_token", "scope")
}
