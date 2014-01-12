package com.jpiche.redditroulette.fragments

import scala.concurrent.ExecutionContext.Implicits.global
import com.jpiche.redditroulette.TypedResource._
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.{Bundle, Handler}
import com.squareup.picasso.{Callback, Picasso}
import android.net.Uri
import android.util.Log
import com.jpiche.redditroulette._
import com.jpiche.redditroulette.reddit.Thing
import android.content.Context
import com.jpiche.redditroulette.net.{WebFail, WebData, Web}
import scala.util.{Failure, Success}

final case class ImageFragment() extends ThingFragment {

  private lazy val picasso = Picasso `with` getActivity

//  private trait ImgCallback extends Callback {
//    def onSuccess() {
//      listener map { _.onFinished() }
//      return
//    }
//  }

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
        handler.post(new Runnable {
          def run() {
            img.setImageBitmap(web.toBitmap)
          }
        })
        listener map { _.onFinished() }

      case Success(fail: WebFail) =>
        Log.e(LOG_TAG, fail.errorMessage)
        listener map { _.onError(thing) }

      case Failure(e) =>
        Log.e(LOG_TAG, s"api exception loading url (${thing.get.goodUrl}: $e")
        listener map { _.onError(thing) }
    }

    /*
    picasso.load(Uri.parse(thing.get.goodUrl))
      .into(img, new ImgCallback {
      def onError() {
        Log.w(LOG_TAG, s"Error loading good url: ${thing.get.goodUrl}")

        if (thing.get.goodUrl == thing.get.url) {
          listener map { _.onError(thing) }

        } else {
          Log.w(LOG_TAG, s"Trying again with url from API: ${thing.get.url}")
          picasso.load(Uri.parse(thing.get.url)).into(img, new ImgCallback {
            def onError() {
              listener map { _.onError(thing) }
              return
            }
          })
        }
        return
      }
    })
    */

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
