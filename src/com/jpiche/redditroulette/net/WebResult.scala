package com.jpiche.redditroulette.net

import java.net.HttpURLConnection
import scalaz._, Scalaz._
import com.jpiche.redditroulette.LogTag
import android.graphics.Bitmap

case class ContentType(conn: HttpURLConnection) extends ContentTypeParser {
  private lazy val parsedContentType = parseContentType(conn.getContentType)

  val rawContentType = conn.getContentType

  lazy val rawCharset: Option[String] = parsedContentType match {
    case Some((_, Some(x))) => x.some
    case _ => None
  }
  lazy val charset = rawCharset | "UTF-8"

  lazy val (contentType: String, isImage: Boolean) = parsedContentType match {
    case None => ("", false)
    case Some((-\/(x), _)) => (x, false)
    case Some((\/-(("image", x)), _)) => (s"image/${x.toLowerCase}", true)
    case Some((\/-((a, x)), _)) => (s"$a/$x", true)
  }

  lazy val isGif = contentType == "image/gif"
}


sealed trait WebResult {
  val conn: HttpURLConnection

  lazy val status = conn.getResponseCode
  lazy val length = conn.getContentLength
  lazy val url = conn.getURL
  lazy val contentType = ContentType(conn)
}

case class WebFail(conn: HttpURLConnection) extends WebResult {
  lazy val errorMessage = s"Connection for url (${conn.getURL.toString}) failed with status: $status"
}

case class WebData(conn: HttpURLConnection, data: Array[Byte], hash: Option[String] = None) extends WebResult with WebBitmap with LogTag {
  lazy val asString = new String(data, contentType.charset)
  lazy val toBitmap: Option[Bitmap] = toBitmap(data)
}
