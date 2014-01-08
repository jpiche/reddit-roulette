package com.jpiche.redditroulette


case class XorString(in: String) {
  lazy val bigInt = BigInt(in.getBytes)
  override val toString = in

  def ^(key: String): String = ^(BigInt(key.getBytes))
  def ^(key: BigInt): String = {
    val enc = bigInt ^ key
    new String(enc.toByteArray)
  }
}