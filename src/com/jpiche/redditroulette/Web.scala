package com.jpiche.redditroulette

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Future, future}
import com.squareup.okhttp.OkHttpClient
import java.net.URL
import java.io.{ByteArrayOutputStream, InputStream}
import scala.annotation.tailrec
import android.util.Log

object Web {
  private val BUFFER_SIZE = 0x1000

  private lazy val LOG_TAG = this.getClass.getSimpleName
  private lazy val client = new OkHttpClient

  def get(url: String): Future[String] = {
    get(new URL(url))
  }

  def get(url: URL): Future[String] = future {

    val conn = client.open(url)
    conn.setRequestProperty("User-Agent", "redditroulette/0.1 by jpiche")

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
      Log.d(LOG_TAG, "read %d bytes" format count)
      if (count != -1) {
        out.write(buffer, 0, count)
        read()
      }
    }
    read()
    out.toByteArray
  }
}
