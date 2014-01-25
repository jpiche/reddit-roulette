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

  private final val BUFFER_SIZE = 0x4000

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

    if (status >= 400) {
      conn.disconnect()
      WebFail(conn)

    } else {
      val in = conn.getInputStream
      try {

          val (resp, hash) = readFullAndHash(in)
          WebData(conn, resp, Some(hash))

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

  def unescape(text:String) = {
    @tailrec def un(textList: List[Char], acc: String, escapeFlag: Boolean): String =
      textList match {
        case Nil => acc
        case '&'::tail => un(tail,acc,true)
        case ';'::tail if escapeFlag => un(tail,acc,false)
        case 'a'::'m'::'p'::tail if escapeFlag => un(tail,acc+"&",true)
        case 'q'::'u'::'o'::'t'::tail if escapeFlag => un(tail,acc+"\"",true)
        case 'l'::'t'::tail if escapeFlag => un(tail,acc+"<",true)
        case 'g'::'t'::tail if escapeFlag => un(tail,acc+">",true)
        case x::tail => un(tail,acc+x,true)
        case _ => acc
      }
    un(text.toList,"",false)
  }
}
