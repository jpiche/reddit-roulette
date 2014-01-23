package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, promise, future}
import scala.util.{Failure, Success, Random}
import com.jpiche.redditroulette._
import android.util.Log
import scala.Some
import com.jpiche.redditroulette.net._
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable

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
    Subreddit.listCache.get(name) match {
      // TODO: make the number a variable somewhere?
      case Some((web, listing)) if Subreddit.listCacheInc.getOrElse(name, 0) < 20 =>
        future {
          // increment the number of times we've used this
          // TODO: is this thread safe?
          Subreddit.listCacheInc put (name, Subreddit.listCacheInc.getOrElse(name, 1))
          Subreddit.listCache put (name, (web, listing))
          (web, Subreddit.randomThing(listing))
        }
      case _ =>
        Web.get(hot) collect {
          case web: WebData =>
            web.asString.decodeOption[Listing] match {
              case Some(list) if list.hasChildren =>
                // every time this is re-fetched, reset the counter to zero
                Subreddit.listCacheInc put (name, 0)
                Subreddit.listCache put (name, (web, list))
                (web, Subreddit.randomThing(list))
              case None => throw new Exception
            }

          case fail: WebFail => throw new Exception(fail.errorMessage)
        }
    }
}

object Subreddit extends LogTag {

  private val listCache = mutable.HashMap.empty[String, (WebData, Listing)]
  private val listCacheInc = mutable.HashMap.empty[String, Int]

  private def aboutUrl(name: CharSequence) = s"http://www.reddit.com/r/$name/about.json"

  /**
   * This default list of subreddits have been hand-picked from top subscriber
   * lists to give a reflection of various image-heavy subs.
   */
  val defaultSubs = List(
    Subreddit("pics"),
    Subreddit("funny"),
    Subreddit("aww"),
    Subreddit("EarthPorn"),
    Subreddit("spaceporn"),
    Subreddit("gifs"),
    Subreddit("FoodPorn"),
    Subreddit("AbandonedPorn"),
    Subreddit("worldnews"),
    Subreddit("ArtPorn"),
    Subreddit("wtf"),
    Subreddit("cringepics"),
    Subreddit("humanporn"),
    Subreddit("gonewild", nsfw = true),
    Subreddit("nsfw", nsfw = true),
    Subreddit("fiftyfifty", nsfw = true),
    Subreddit("ginger", nsfw = true),
    Subreddit("RealGirls", nsfw = true)
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

  def randomThing(listing: Listing)(implicit prefs: Prefs): Thing = {
    val children = if (prefs.showSelf)
      listing.children
    else
      listing.children filterNot { _.data.isSelf }

    val t = {
      val i = Random.nextInt(children.length)
      children(i).data
    }

    Log.d(LOG_TAG, "collecting thing with url: %s" format t.url)
    t
  }
}