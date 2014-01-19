package com.jpiche.redditroulette.reddit

import argonaut._, Argonaut._

case class Listing(
  kind: String,
  data: ListingData
) {
  val children = data.children
  val hasChildren = ! children.isEmpty
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
