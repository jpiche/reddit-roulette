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
  val isImg: Boolean = {
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

  val goodUrl: String = if (isImg) {
    val imgur = """imgur\.com/(?:gallery/)?([a-zA-Z0-9]+)$""".r.unanchored
    val livememe = """livememe\.com/([a-zA-Z0-9]+)\.?""".r.unanchored

    url match {
      case imgur(key) => "http://i.imgur.com/%s.jpg" format key
      case livememe(key) => "http://i.lvme.com/%s.jpg" format key
      case _ => url
    }
  } else url

  val r_sub = "/r/%s" format subreddit

  lazy val toBundle = {
    val b = new Bundle(1)
    b.putString(Thing.THING_KEY, this.asJson.nospaces)
    b
  }
}
object Thing {
  private val THING_KEY = "THING_KEY"

  def apply(bundle: Bundle): Option[Thing] =
    if (bundle.containsKey(THING_KEY))
      bundle.getString(THING_KEY).decodeOption[Thing]
    else
      None

  implicit def ThingCodecJson: CodecJson[Thing] =
    casecodec14(Thing.apply, Thing.unapply)("domain", "subreddit", "id", "author", "score", "over_18", "is_self", "permalink", "name", "url", "title", "created_utc", "num_comments", "visited")
}
