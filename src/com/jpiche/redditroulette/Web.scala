package com.jpiche.redditroulette

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Future, future}
import com.squareup.okhttp.OkHttpClient
import java.net.{URLEncoder, URL}
import java.io.{ByteArrayOutputStream, InputStream}
import scala.annotation.tailrec
import android.util.Log

object Web extends LogTag {

  private val BUFFER_SIZE = 0x1000

  private lazy val client = new OkHttpClient

  @inline def get(url: String): Future[String] = {
    get(new URL(url))
  }

  def get(url: URL): Future[String] = future {

    val conn = client.open(url)
    conn.setRequestProperty("User-Agent", RouletteApp.USER_AGENT)

    val in = conn.getInputStream
    try {
      val resp = readFull(in)
      new String(resp, "UTF-8")

    } finally {
      in.close()
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
