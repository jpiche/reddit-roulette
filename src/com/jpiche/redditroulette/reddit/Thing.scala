package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import android.os.Bundle
import android.text.{Html, Spanned}
import org.joda.time.{DateTimeZone, Duration, DateTime}
import com.jpiche.redditroulette.LogTag
import android.util.Log

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
  downs: Int,
  isSelf: Boolean,
  permalink: String,
  name: String,
  url: String,
  title: String,
  created: Long,
  ups: Int,
  comments: Int,
  visited: Boolean
) extends LogTag {
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
  val likelyGif = url.toLowerCase endsWith ".gif"

  val goodUrl: String = if (isImg) {
    val imgur = """imgur\.com/(?:gallery/)?([a-zA-Z0-9]+)$""".r.unanchored
    val livememe = """livememe\.com/([a-zA-Z0-9]+)\.?""".r.unanchored

    url match {
      case imgur(key) => "http://i.imgur.com/%s.jpg" format key
      case livememe(key) => "http://i.lvme.com/%s.jpg" format key
      case _ => url
    }
  } else url

  val r_sub = s"/r/$subreddit"
  val full_permalink = s"http://www.reddit.com$permalink"
  val scoreInfo = s"$score ($ups Up, $downs Down)"

  lazy val toBundle = {
    val b = new Bundle(1)
    b.putString(Thing.THING_KEY, this.asJson.nospaces)
    b
  }

  lazy val timeHuman: String = {
    val now = DateTime.now(DateTimeZone.UTC).getMillis
    val dur = new Duration(created * 1000, now)

    val days = dur.getStandardDays
    val hours = dur.getStandardHours
    val mins = dur.getStandardMinutes

    Log.i(LOG_TAG, s"Duration from (created: $created, now: $now): $days d, $hours h, $mins m")

    val dayWord = if (days == 0) "day" else "days"
    val hourWord = if (hours == 0) "hour" else "hours"
    val minWord = if (mins == 0) "minute" else "minutes"

    if (days > 0) {
      s"$days $dayWord ago"

    } else if (hours > 0) {
      val m = mins - (hours * 60)
      s"$hours $hourWord, $m $minWord ago"

    } else if (mins > 0) {
      s"$mins $minWord ago"
    } else {
      "moments ago"
    }
  }

  lazy val info: Spanned = {
    val s = s"submitted <b>$timeHuman</b> by <b>$author</b> to <b>$subreddit</b>"
    Html.fromHtml(s)
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
    casecodec16(Thing.apply, Thing.unapply)("domain", "subreddit", "id", "author", "score", "over_18", "downs", "is_self", "permalink", "name", "url", "title", "created_utc", "ups", "num_comments", "visited")
}
