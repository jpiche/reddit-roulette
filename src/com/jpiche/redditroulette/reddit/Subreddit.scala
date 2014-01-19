package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Random}
import com.jpiche.redditroulette._
import scala.concurrent.{Future, promise}
import android.util.Log
import scala.Some
import com.jpiche.redditroulette.net._


case class SubredditData(
  kind: String,
  data: Subreddit
)
object SubredditData {
  implicit def SubredditDataCodecJson: CodecJson[SubredditData] =
    casecodec2(SubredditData.apply, SubredditData.unapply)("kind", "data")
}

case class Subreddit(
    name: String,
    nsfw: Boolean = false
) {
  lazy val url = "http://www.reddit.com/r/%s/" format name
  lazy val hot = url + "hot.json?limit=100"

  def next(implicit webSettings: WebSettings, prefs: Prefs): Future[(WebData, Thing)] =
    Web.get(hot) collect {
      case web: WebData => Subreddit.randomThing(web)

      case fail: WebFail => throw new Exception(fail.errorMessage)
    }
}

object Subreddit extends LogTag {

  private def aboutUrl(name: CharSequence) = s"http://www.reddit.com/r/$name/about.json"

  val defaultSubs = List(
    Subreddit("earthporn"),
    Subreddit("spaceporn"),
    Subreddit("aww"),
    Subreddit("puppies"),
    Subreddit("cats"),
    Subreddit("pics"),
    Subreddit("wtf", nsfw = true),
    Subreddit("gonewild", nsfw = true),
    Subreddit("nsfw", nsfw = true),
    Subreddit("ginger", nsfw = true),
    Subreddit("nsfw_hd", nsfw = true)
  )

  implicit def SubredditCodecJson: CodecJson[Subreddit] =
    casecodec2(Subreddit.apply, Subreddit.unapply)("display_name", "over18")

  def retrieve(name: CharSequence)(implicit webSettings: WebSettings): Future[Option[Subreddit]] = {
    val p = promise[Option[Subreddit]]()

    Web.get(aboutUrl(name)) onComplete {
      case Success(web: WebData) =>
        web.asString.decodeOption[SubredditData] match {
          case Some(SubredditData(_, sub)) =>
            p success Some(sub)
          case None =>
            p success None
        }

      case Success(web: WebFail) =>
        p success None

      case Failure(e) =>
        // this doesn't feel right...
        p success None
    }

    p.future
  }

  def randomThing(web: WebData)(implicit prefs: Prefs): (WebData, Thing) =
    web.asString.decodeOption[Listing] match {
      case Some(listing: Listing) if listing.hasChildren =>
        val children = if (prefs.showSelf)
          listing.children
        else
          listing.children filterNot { _.data.isSelf }

        val t = {
          val i = Random.nextInt(children.length)
          children(i).data
        }

        Log.d(LOG_TAG, "collecting thing with url: %s" format t.url)
        (web, t)

      case _ => throw new Exception
    }
}