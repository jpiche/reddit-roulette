package com.jpiche.redditroulette.net

import android.opengl.GLES20
import android.graphics.{BitmapFactory, Bitmap}
import android.util.Log

trait WebBitmap { self =>

  private val LOG_TAG = self.getClass.getSimpleName

  protected def toBitmap(data: Array[Byte]): Bitmap = {
    val bmp = BitmapFactory.decodeByteArray(data, 0, data.length)
    val max = Math.max(bmp.getHeight, bmp.getWidth)

    Log.d(LOG_TAG, s"Image max: $max; OPENGL_MAX: ${WebBitmap.OPENGL_MAX}")

    // If the image is too large to draw, then resize it.
    if (max > WebBitmap.OPENGL_MAX) {
      bmp.recycle()

      val scaleFactorPowerOfTwo: Double = Math.log(max.toDouble / WebBitmap.OPENGL_MAX.toDouble) / Math.log(2)
      val scaleFactor = Math.round(Math.pow(2, Math.ceil(scaleFactorPowerOfTwo)))
      Log.d(LOG_TAG, s"Image resize using scaleFactorPowerOfTwo: $scaleFactorPowerOfTwo, and scaleFactor: $scaleFactor")

      val options = new BitmapFactory.Options()
      options.inSampleSize = scaleFactor.toInt
      BitmapFactory.decodeByteArray(data, 0, data.length, options)

    } else
      bmp
  }
}

object WebBitmap {
  private lazy val OPENGL_MAX: Int = {
    // Build an array with one element, and have that be the default in case
    // glGetIntegerv doesn't actually set a value
    val maxTextureSize = Array(2048)
    GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
    maxTextureSize(0)
  }
}