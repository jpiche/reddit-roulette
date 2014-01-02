package com.jpiche.redditroulette.reddit

import scalaz._, Scalaz._
import argonaut._, Argonaut._
import android.os.Bundle

case class ThingItem(kind: String, data: Thing)
object ThingItem {
  implicit def ThingItemCodecJson: CodecJson[ThingItem] =
    casecodec2(ThingItem.apply, ThingItem.unapply)("kind", "data")
}

case class Thing(
  domain: String,
  subreddit: String,
  id: String,
  author: String,
  score: Int,
  over18: Boolean,
  isSelf: Boolean,
  permalink: String,
  name: String,
  url: String,
  title: String,
  created: Long,
  comments: Int,
  visited: Boolean
) {
  lazy val isImg: Boolean = {
    val imgurAlbum = """imgur\.com/a/[a-zA-Z0-9]+$""".r.unanchored

    val d = List(
      "i.imgur.com",
      "i.qkme.me",
      "imgur.com",
      "i.lvme.me",
      "livememe.com"
    ) contains domain

    // don't load albums as images
    val notAlbum = url match {
      case imgurAlbum() => false
      case _ => true
    }

    val u = List(".jpg", ".jpeg", ".png") exists { x => url endsWith x }
    (d && notAlbum) || u
  }

  lazy val goodUrl: String = if (isImg) {
    val imgur = """imgur\.com/(?:gallery/)?([a-zA-Z0-9]+)$""".r.unanchored
    val livememe = """livememe\.com/([a-zA-Z0-9]+)\.?""".r.unanchored

    url match {
      case imgur(key) => "http://i.imgur.com/%s.jpg" format key
      case livememe(key) => "http://i.lvme.com/%s.jpg" format key
      case _ => url
    }
  } else url

  lazy val r_sub = "/r/%s" format subreddit

  lazy val toBundle = {
    val b = new Bundle()
    b.putString(Thing.KEY_DOMAIN, domain)
    b.putString(Thing.KEY_SUBREDDIT, subreddit)
    b.putString(Thing.KEY_ID, id)
    b.putString(Thing.KEY_AUTHOR, author)
    b.putInt(Thing.KEY_SCORE, score)
    b.putBoolean(Thing.KEY_OVER18, over18)
    b.putBoolean(Thing.KEY_ISSELF, isSelf)
    b.putString(Thing.KEY_PERMALINK, permalink)
    b.putString(Thing.KEY_NAME, name)
    b.putString(Thing.KEY_URL, url)
    b.putString(Thing.KEY_TITLE, title)
    b.putLong(Thing.KEY_CREATED, created)
    b.putInt(Thing.KEY_COMMENTS, comments)
    b.putBoolean(Thing.KEY_VISITED, visited)
    b
  }
}
object Thing {

  val KEY_DOMAIN = "thing_domain"
  val KEY_SUBREDDIT = "thing_subreddit"
  val KEY_ID = "thing_id"
  val KEY_AUTHOR = "thing_author"
  val KEY_SCORE = "thing_score"
  val KEY_OVER18 = "thing_over18"
  val KEY_ISSELF = "thing_isself"
  val KEY_PERMALINK = "thing_permalink"
  val KEY_NAME = "thing_name"
  val KEY_URL = "thing_url"
  val KEY_TITLE = "thing_title"
  val KEY_CREATED = "thing_created"
  val KEY_COMMENTS = "thing_comments"
  val KEY_VISITED = "thing_visited"

  // bundles are kinda awful, but at least it maintains types (sorta)
  def apply(bundle: Bundle): Option[Thing] =
    if (bundle.containsKey(KEY_ID)
      && bundle.containsKey(KEY_URL)
      && bundle.containsKey(KEY_TITLE)
    )
      Thing(
        domain = bundle.getString(KEY_DOMAIN, ""),
        subreddit = bundle.getString(KEY_SUBREDDIT, ""),
        id = bundle.getString(KEY_ID),
        author = bundle.getString(KEY_AUTHOR, ""),
        score = bundle.getInt(KEY_SCORE, 0),
        over18 = bundle.getBoolean(KEY_OVER18, false),
        isSelf = bundle.getBoolean(KEY_ISSELF, false),
        permalink = bundle.getString(KEY_PERMALINK, ""),
        name = bundle.getString(KEY_NAME, ""),
        url = bundle.getString(KEY_URL),
        title = bundle.getString(KEY_TITLE),
        created = bundle.getLong(KEY_CREATED, 0),
        comments = bundle.getInt(KEY_COMMENTS, 0),
        visited = bundle.getBoolean(KEY_VISITED, false)
      ).some
    else
      None

  implicit def ThingCodecJson: CodecJson[Thing] =
    casecodec14(Thing.apply, Thing.unapply)("domain", "subreddit", "id", "author", "score", "over_18", "is_self", "permalink", "name", "url", "title", "created_utc", "num_comments", "visited")
}
