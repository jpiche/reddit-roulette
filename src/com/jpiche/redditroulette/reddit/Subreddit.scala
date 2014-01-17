package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import com.jpiche.redditroulette._
import scala.concurrent.{Future, future}
import android.util.Log
import scala.Some
import com.jpiche.redditroulette.net._


case class Subreddit(
    name: String,
    nsfw: Boolean = false,
    use: Boolean = true) {

  lazy val url = "http://www.reddit.com/r/%s/" format name
  lazy val hot = url + "hot.json?limit=100"

  def next(implicit webSettings: WebSettings, prefs: Prefs): Future[(WebData, Thing)] =
    Web.get(hot) collect {
      case web: WebData => Subreddit.randomThing(web)

      case fail: WebFail => throw new Exception(fail.errorMessage)
    }
}

object Subreddit extends LogTag {

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

  def random(implicit db: Db, prefs: Prefs): Future[Subreddit] = future {
    val subs = db allSubs prefs.allowNsfw
    Log.i(LOG_TAG, s"subs from db: $subs")

    val i = Random.nextInt(subs.size)
    val s = subs(i)
    Log.i(LOG_TAG, s"picked sub: $s")
    s
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