package com.jpiche.redditroulette.net

import java.net.HttpURLConnection
import scalaz._, Scalaz._
import android.graphics.{Bitmap, BitmapFactory}
import com.jpiche.redditroulette.LogTag
import android.util.Log
import android.opengl.GLES20


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
  lazy val OPENGL_MAX: Int = {
    // Build an array with one element, and have that be the default in case
    // glGetIntegerv doesn't actually set a value
    val maxTextureSize = Array(2048)
    GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
    maxTextureSize(0)
  }

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
    val bmp = BitmapFactory.decodeByteArray(data, 0, data.length)
    val max = Math.max(bmp.getHeight, bmp.getWidth)

    Log.d(LOG_TAG, s"Image max: $max; OPENGL_MAX: $OPENGL_MAX")

    // If the image is too large to draw, then resize it.
    if (max > OPENGL_MAX) {
      bmp.recycle()

      val scaleFactorPowerOfTwo: Double = Math.log(max.toDouble / OPENGL_MAX.toDouble) / Math.log(2)
      val scaleFactor = Math.round(Math.pow(2, Math.ceil(scaleFactorPowerOfTwo)))
      Log.d(LOG_TAG, s"Image resize using scaleFactorPowerOfTwo: $scaleFactorPowerOfTwo, and scaleFactor: $scaleFactor")

      val options = new BitmapFactory.Options()
      options.inSampleSize = scaleFactor.toInt
      BitmapFactory.decodeByteArray(data, 0, data.length, options)

    } else
      bmp
  }
}