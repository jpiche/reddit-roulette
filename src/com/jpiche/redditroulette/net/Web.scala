package com.jpiche.redditroulette.net

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Future, future}
import com.squareup.okhttp.OkHttpClient
import java.net.URL
import java.io.{ByteArrayOutputStream, InputStream}
import scala.annotation.tailrec
import android.util.Log
import com.jpiche.redditroulette.LogTag

sealed trait Web {
  val url: URL
  def progress(f: (Float) => Unit): Web
}

object Web extends LogTag {

  private class WebImpl

  private final val BUFFER_SIZE = 0x1000

  private lazy val client = new OkHttpClient

  def get(url: String)(implicit settings: WebSettings): Future[WebResult] =
    get(new URL(url))

  private def get(url: URL)(implicit settings: WebSettings): Future[WebResult] = future {

    val conn = client.open(url)
    conn.setUseCaches(true)
    conn.setRequestProperty("User-Agent", settings.userAgent)

    val status = conn.getResponseCode
    val headers = conn.getHeaderFields
    Log.d(LOG_TAG, s"header fields for url (${url.toString}}): ${headers.toString}")

    if (status >= 400) {
      conn.disconnect()
      WebFail(conn)

    } else {
      val in = conn.getInputStream
      try {
        val resp = readFull(in)
        WebData(conn, resp)

      } finally {
        in.close()
      }
    }
  }

  private def readFull(in: InputStream): Array[Byte] = {
    val out = new ByteArrayOutputStream(BUFFER_SIZE)
    val buffer = new Array[Byte](BUFFER_SIZE)

    @tailrec
    def read() {
      val count = in.read(buffer)
      if (count != -1) {
        out.write(buffer, 0, count)
        read()
      }
    }
    read()
    out.toByteArray
  }
}
