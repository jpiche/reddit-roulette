package com.jpiche

package object redditroulette {

  implicit def toXorString(orig: String) = XorString(orig)

  implicit class XorStringHelper(val sc: StringContext) extends AnyVal {
    def x(args: Any*): XorString = XorString(new String(BigInt(sc.parts.mkString).toByteArray))
  }
}
