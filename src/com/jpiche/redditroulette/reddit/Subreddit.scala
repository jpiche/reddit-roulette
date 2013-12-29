package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import com.jpiche.redditroulette.Web
import scala.concurrent.Future
import android.util.Log

case class Subreddit(name: String) {
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

  val availableSubs = List(
    "earthporn",
    "aww",
    "puppies",
    "cats",
    "spaceporn",
    "worldnews",
    "nsfw",
    "wtf"
  )

  def random: Subreddit = {
    val i = Random.nextInt(availableSubs.length)
    Subreddit(availableSubs(i))
  }
}