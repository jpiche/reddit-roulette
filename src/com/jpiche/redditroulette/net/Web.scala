package com.jpiche.redditroulette.net

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, future}
import com.squareup.okhttp.OkHttpClient
import java.net.URL
import java.io.{ByteArrayOutputStream, InputStream}
import scala.annotation.tailrec
import android.util.Log
import com.jpiche.redditroulette.LogTag
import java.security.MessageDigest

sealed trait Web {
  val url: URL
  def progress(f: (Float) => Unit): Web
}

object Web extends LogTag {

  private class WebImpl

  private final val BUFFER_SIZE = 0x1000

  private lazy val client = new OkHttpClient

  def get(url: String)(implicit settings: WebSettings): Future[WebResult] =
    http(new URL(url), "GET")

  def post(url: String)(implicit settings: WebSettings): Future[WebResult] =
    http(new URL(url), "POST")

  private def http(url: URL, method: String)(implicit settings: WebSettings): Future[WebResult] = future {

    val conn = client.open(url)
    conn.setUseCaches(true)
    conn.setRequestProperty("User-Agent", settings.userAgent)
    conn.setRequestMethod(method)

    val status = conn.getResponseCode
//    val headers = conn.getHeaderFields
//    Log.d(LOG_TAG, s"header fields for url (${url.toString}}): ${headers.toString}")

    if (status >= 400) {
      conn.disconnect()
      WebFail(conn)

    } else {
//      val contentType = ContentType(conn)
      val in = conn.getInputStream
      try {

//        if (contentType.isImage) {
          val (resp, hash) = readFullAndHash(in)
          WebData(conn, resp, Some(hash))
//        } else {
//          val resp = readFull(in)
//          WebData(conn, resp)
//        }

      } finally {
        in.close()
      }
    }
  }

  private def readFullAndHash(in: InputStream): (Array[Byte], String) = {
    val out = new ByteArrayOutputStream(BUFFER_SIZE)
    val buffer = new Array[Byte](BUFFER_SIZE)
    val md = MessageDigest.getInstance("SHA1")

    @tailrec
    def read() {
      val count = in.read(buffer)
      if (count != -1) {
        out.write(buffer, 0, count)
        md.update(buffer, 0, count)
        read()
      }
    }
    read()
    val byteHash: Array[Byte] = md.digest()
    val hash: String = byteHash.map("%02X" format _).mkString
    Log.d(LOG_TAG, s"hash for byte array: $hash")
    (out.toByteArray, hash)
  }
//
//  private def readFull(in: InputStream): Array[Byte] = {
//    val out = new ByteArrayOutputStream(BUFFER_SIZE)
//    val buffer = new Array[Byte](BUFFER_SIZE)
//
//    @tailrec
//    def read() {
//      val count = in.read(buffer)
//      if (count != -1) {
//        out.write(buffer, 0, count)
//        read()
//      }
//    }
//    read()
//    out.toByteArray
//  }
}
