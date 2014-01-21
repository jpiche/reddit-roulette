package com.jpiche.redditroulette.fragments

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.util.Success

import android.view.{LayoutInflater, ViewGroup, View}
import android.os.{Handler, Bundle}
import android.util.Log

import com.jpiche.redditroulette.TypedResource._
import com.jpiche.redditroulette._
import com.jpiche.redditroulette.reddit.Thing
import com.jpiche.redditroulette.net.{WebBitmapData, WebData, WebBitmap}


final case class ImageFragment() extends ThingFragment {

  private lazy val flingListener = new FlingListener {
    def onFling(dir: FlingDirection) {
      Log.d(LOG_TAG, s"fling: ${dir.toString}")
      dir match {
        case FlingLeft => listener map { _.onNext() }
        case FlingRight => listener map { _.onPrev() }
        case _ =>
      }
      return
    }
  }

  private var webData: Option[WebBitmap] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null && args.containsKey(ImageFragment.KEY_DATA)) {
      val d = args.getByteArray(ImageFragment.KEY_DATA)
      webData = Some(WebBitmapData(d))
    }
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    Log.d(LOG_TAG, s"onCreateView with thing $thing")

    if (thing.isEmpty || webData.isEmpty) {
      listener map { _.onError(thing) }
      return null
    }

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_image, container, attachToRoot)

    val img = v findView TR.imageView
    val prog = v findView TR.progressLayout
    prog setVisibility View.VISIBLE

    future {
      try {
        webData.get.toBitmap

      } catch {
        case oom: OutOfMemoryError =>
          Log.w(LOG_TAG, s"OutOfMemoryError with thing $thing")
          listener map { _.onError(thing) }
          None
      }
    } onComplete {
      case Success(Some(bmp)) =>
        run {
          img setImageBitmap bmp
          prog setVisibility View.GONE
        }

      case _ =>
        listener map { _.onError(thing) }
    }

    v
  }

  override def onResume() {
    super.onResume()

    if (getView != null) {
      val img = getView.findView(TR.imageView)
      img.setOnTouchListener {
        if (prefs.swipeLeftNext)
          flingListener
        else
          null
      }
    }
  }
}

object ImageFragment extends FragTag {
  private final val KEY_DATA = "__DATA"

  def apply(listener: ThingListener, thing: Thing, web: WebData): ImageFragment = {
    val frag = new ImageFragment()
    frag.listener = Some(listener)

    val b = thing.toBundle
    b.putByteArray(KEY_DATA, web.data)

    frag.setArguments(b)
    frag
  }
}
