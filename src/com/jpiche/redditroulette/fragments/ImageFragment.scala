package com.jpiche.redditroulette.fragments

import scala.concurrent.ExecutionContext.Implicits.global
import com.jpiche.redditroulette.TypedResource._
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.{Bundle, Handler}
import android.util.Log
import com.jpiche.redditroulette._
import com.jpiche.redditroulette.reddit.Thing
import com.jpiche.redditroulette.net.{WebFail, WebData, Web}
import scala.util.{Failure, Success}


final case class ImageFragment() extends ThingFragment {

  private lazy val flingListener = new FlingListener {
    def onFling(dir: FlingDirection) {
      Log.d(LOG_TAG, s"fling: ${dir.toString}")
      dir match {
        case FlingLeft => listener map { _.onNext() }
        case _ =>
      }
      return
    }
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    if (thing.isEmpty) {
      listener map { _.onError(thing) }
      return null
    }

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_image, container, attachToRoot)
    val img = v.findView(TR.imageView)

    val handler = new Handler()

    Web.get(thing.get.goodUrl) onComplete {
      case Success(web: WebData) =>
        try {
          val bmp = web.toBitmap
          handler.post(new Runnable {
            def run() {
              img.setImageBitmap(bmp)
            }
          })
          listener map { _.onFinished() }

        } catch {
          case e: OutOfMemoryError =>
            Log.e(LOG_TAG, s"OutOfMemoryError with url (${thing.get.goodUrl}: $e")
            listener map { _.onError(thing) }
        }

      case Success(fail: WebFail) =>
        Log.e(LOG_TAG, fail.errorMessage)
        listener map { _.onError(thing) }

      case Failure(e) =>
        Log.e(LOG_TAG, s"api exception loading url (${thing.get.goodUrl}: $e")
        listener map { _.onError(thing) }
    }

    v
  }

  override def onResume() {
    super.onResume()

    val img = getView.findView(TR.imageView)
    img.setOnTouchListener {
      if (prefs.swipeLeftNext)
        flingListener
      else
        null
    }
  }
}

object ImageFragment extends FragTag {

  def apply(listener: Option[ThingListener], thing: Thing): ImageFragment = {
    val frag = new ImageFragment()
    frag.listener = listener
    frag.setArguments(thing.toBundle)
    frag
  }
}
