package com.jpiche.redditroulette.net

import scala.util.parsing.combinator._
import scalaz._, Scalaz._

trait ContentTypeParser extends JavaTokenParsers {
  private lazy val mime = (stringLiteral ~ "/" ~ stringLiteral) | stringLiteral
  private lazy val charset = ";" ~> "charset" ~> "=" ~> opt("\"" | "'") ~> stringLiteral <~ opt("\"" | "'")

  private lazy val full: Parser[(\/[String, (String, String)], Option[String])] = mime ~ opt(charset) ^^ {
    case (m: String) ~ c => (m.left[(String, String)] , c)
    case (ml: String) ~ "/" ~ (mr: String) ~ c => ((ml, mr).right[String], c)
  }

  def parseContentType(in: CharSequence): Option[(\/[String, (String, String)], Option[String])] = parseAll(full, in) match {
    case Success(result, _) => result.some
    case _ => None
  }
}
