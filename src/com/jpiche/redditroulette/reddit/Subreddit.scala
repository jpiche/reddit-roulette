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
  private lazy val LOG_TAG = this.getClass.getSimpleName

  lazy val url = "http://www.reddit.com/r/%s/" format name
  lazy val hot = url + "hot.json"

  def next(implicit webSettings: WebSettings, prefs: Prefs): Future[(WebData, Thing)] =
    Web.get(hot) collect {
      case web@WebData(_, _) =>
        web.asString.decodeOption[Listing] match {
          case Some(listing: Listing) if listing.hasChildren => {
            val children = if (prefs.showSelf)
              listing.children
            else
              listing.children filterNot { _.data.isSelf }

            val t = {
              // TODO: Save off selected thing and don't grab it again for a bit.
              val i = Random.nextInt(children.length)
              children(i).data
            }

            Log.d(LOG_TAG, "collecting thing with url: %s" format t.url)
            (web, t)
          }
          case None => throw new Exception
        }
      case fail@WebFail(_) => throw new Exception(fail.errorMessage)
    }
}

object Subreddit {

  private val defaultSubs = List(
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
    if (db.countSubs == 0) {
      db add defaultSubs
    }
    val subs = db allSubs prefs.allowNsfw

    val i = Random.nextInt(subs.size)
    subs(i)
  }
}