package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._
import scala.util.Random

case class Listing(
  kind: String,
  data: ListingData
) {
  // TODO: this should probably be a setting
  lazy val children = data.children filterNot { _.data.isSelf }

  val hasChildren = ! children.isEmpty

  def random: Thing = {
    val i = Random.nextInt(children.length)
    children(i).data
  }
}
object Listing {
  implicit def ListingCodecJson: CodecJson[Listing] =
    casecodec2(Listing.apply, Listing.unapply)("kind", "data")
}

case class ListingData(
  modhash: String,
  after: Option[String] = None,
  before: Option[String] = None,
  children: List[ThingItem] = Nil
)
object ListingData {
  implicit def ListingDataCodecJson: CodecJson[ListingData] =
    casecodec4(ListingData.apply, ListingData.unapply)("modhash", "after", "before", "children")
}
