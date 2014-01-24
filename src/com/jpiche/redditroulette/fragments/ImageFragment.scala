package com.jpiche.redditroulette.fragments


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, promise, future}
import scala.util.{Failure, Success}

import android.view.{LayoutInflater, ViewGroup, View}
import android.os.Bundle
import android.util.Log
import android.graphics.{Movie, Bitmap}

import com.jpiche.redditroulette.TypedResource._
import com.jpiche.redditroulette.{R, TR, FragTag, LogTag}
import com.jpiche.redditroulette.reddit.Thing
import com.jpiche.redditroulette.net.{BitmapData, WebData}
import com.jpiche.redditroulette.views.GIFView


final case class ImageFragment() extends ThingFragment {

  private var data: Option[Array[Byte]] = None
  private var pData: Option[Promise[Either[Bitmap,Movie]]] = None
  private var endData: Option[Either[Bitmap,Movie]] = None
  private var isGif = false

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      if (args.containsKey(ImageFragment.KEY_DATA)) {
        val bmp = args.getByteArray(ImageFragment.KEY_DATA)
        data = Some(bmp)
      }

      position = args.getInt(ThingFragment.KEY_POSITION, position)
      isGif = args.getBoolean(ImageFragment.KEY_GIF, isGif)
    }

    if (data.isEmpty) {
      Log.w(LOG_TAG, s"Data is empty in position ($position) with thing $thing")
      listener map { _.onError(position, thing) }
      return
    }

    val d = data.get

    pData match {
      case Some(_) =>
      case None =>
        val p = promise[Either[Bitmap,Movie]]() completeWith {
          future {
            if (isGif) {
              val m = Movie.decodeByteArray(d, 0, d.length)
              if (m.duration() < 1) {
                throw new RuntimeException("Invalid GIF")
              } else {
                Right(m)
              }
            } else {
              try {
                val web = BitmapData(d)
                web.toBitmap match {
                  case Some(x) => Left(x)
                  case None => throw new Exception
                }
              } catch {
                case oom: OutOfMemoryError =>
                  Log.w(LOG_TAG, s"OutOfMemoryError with thing $thing")
                  throw oom
              }
            }
          }
        }
        pData = Some(p)
    }
    return
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_image, container, attachToRoot)

    val img = v findView TR.imageView
    val prog = v findView TR.progressLayout
    val loadText = v findView TR.loadingText
    loadText setText R.string.loading_image

    pData map {
      _.future onComplete {
        case Success(Left(bmp)) =>
          run {
            img setImageBitmap bmp
            prog setVisibility View.GONE
          }
          endData = Some(Left(bmp))

        case Success(Right(gif)) =>
          run {
            val gifView = new GIFView(thisContext, gif)
            val params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            v.addView(gifView, params)

            img setVisibility View.GONE
            prog setVisibility View.GONE
          }
          endData = Some(Right(gif))

        case Failure(e) =>
          Log.w(LOG_TAG, s"Error loading image in position ($position) from thing: $thing")
          Log.w(LOG_TAG, e)
          listener map { _.onError(position, thing) }
      }
    }

    v
  }
}

object ImageFragment extends FragTag with LogTag {
  private final val KEY_DATA = "__DATA"
  private final val KEY_GIF = "__GIF"

  def apply(position: Int, thing: Thing, web: WebData): ImageFragment = {

    val frag = new ImageFragment()

    val b = thing.toBundle
    b.putInt(ThingFragment.KEY_POSITION, position)
    b.putByteArray(KEY_DATA, web.data)
    b.putBoolean(KEY_GIF, web.contentType.isGif)

    frag.setArguments(b)
    frag
  }
}
