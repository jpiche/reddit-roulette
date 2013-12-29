package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._

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
    val d = List("i.imgur.com", "i.qkme.me") contains domain
    val u = List(".jpg", ".jpeg", ".png") exists { x => url endsWith x }
    d || u
  }
}
object Thing {
  implicit def ThingCodecJson: CodecJson[Thing] =
    casecodec14(Thing.apply, Thing.unapply)("domain", "subreddit", "id", "author", "score", "over_18", "is_self", "permalink", "name", "url", "title", "created_utc", "num_comments", "visited")
}
