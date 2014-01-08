package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import com.jpiche.redditroulette.{Prefs, Db, Web}
import scala.concurrent.{Future, future}
import android.util.Log


case class Subreddit(
    name: String,
    nsfw: Boolean = false,
    use: Boolean = true) {
  private lazy val LOG_TAG = this.getClass.getSimpleName

  lazy val url = "http://www.reddit.com/r/%s/" format name
  lazy val hot = url + "hot.json"

  def next: Future[Thing] =
    Web.get(hot) map {
      _.decodeOption[Listing] match {
        case Some(listing: Listing) if listing.hasChildren => {
          val t = listing.random
          Log.d(LOG_TAG, "collecting thing with url: %s" format t.url)
          t
        }
        case None => throw new Exception
      }
    }

}

object Subreddit {

  private val defaultSubs = List(
    Subreddit("earthporn"),
    Subreddit("spaceporn"),
    Subreddit("aww"),
    Subreddit("puppies"),
    Subreddit("cats"),
    Subreddit("wtf", nsfw = true),
    Subreddit("gonewild", nsfw = true),
    Subreddit("nsfw", nsfw = true),
    Subreddit("ginger", nsfw = true)
  )

  def random(implicit db: Db, prefs: Prefs): Future[Subreddit] = future {
    if (db.countSubs == 0) {
      db add defaultSubs
    }
    val subs = db allSubs prefs.allowNsfw

    val i = Random.nextInt(subs.size)
    subs(i)
  }
}