package com.jpiche.redditroulette.net

import java.net.HttpURLConnection
import scalaz._, Scalaz._
import android.graphics.{Bitmap, BitmapFactory}
import com.jpiche.redditroulette.LogTag
import android.util.Log


sealed trait WebResult {
  val conn: HttpURLConnection

  lazy val status = conn.getResponseCode
  lazy val length = conn.getContentLength
}

case class WebFail(conn: HttpURLConnection) extends WebResult {
  lazy val errorMessage = s"Connection for url (${conn.getURL.toString}) failed with status: $status"
}

case class WebData(conn: HttpURLConnection, data: Array[Byte]) extends WebResult with ContentTypeParser with LogTag {
  lazy val asString = new String(data, charset | "UTF-8")

  private lazy val parsedContentType = parseContentType(conn.getContentType)
  lazy val charset: Option[String] = parsedContentType match {
    case Some((_, Some(x))) => x.some
    case _ => None
  }
  lazy val (contentType: String, isImage: Boolean) = parsedContentType match {
    case None => ("", false)
    case Some((-\/(x), _)) => (x, false)
    case Some((\/-(("image", x)), _)) => (s"image/$x", true)
    case Some((\/-((a, x)), _)) => (s"$a/$x", true)
  }

  def toBitmap: Bitmap = {
    val OPENGL_MAX = 2048.0
    val bmp = BitmapFactory.decodeByteArray(data, 0, data.length)
    val max = Math.max(bmp.getHeight, bmp.getWidth)

    // If the image is too large to draw, then resize it.
    if (max > OPENGL_MAX) {
      Log.d(LOG_TAG, s"Image max: $max, resizing to $OPENGL_MAX")
      bmp.recycle()

      val scaleFactorPowerOfTwo: Double = Math.log(max / OPENGL_MAX) / Math.log(2)
      val scaleFactor = Math.round(Math.pow(2, Math.ceil(scaleFactorPowerOfTwo)))
      Log.d(LOG_TAG, s"Image resize using scaleFactorPowerOfTwo: $scaleFactorPowerOfTwo, and scaleFactor: $scaleFactor")

      val options = new BitmapFactory.Options()
      options.inSampleSize = scaleFactor.toInt
      BitmapFactory.decodeByteArray(data, 0, data.length, options)

    } else
      bmp
  }
}