package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.Bundle
import android.util.Log
import com.jpiche.redditroulette._
import com.jpiche.redditroulette.reddit.Thing
import com.jpiche.redditroulette.net.{WebBitmapData, WebData, WebBitmap}


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
    val img = v.findView(TR.imageView)

    webData.get.toBitmap match {
      case Some(bmp) =>
        img setImageBitmap bmp
        listener map { _.onFinished() }

      case None =>
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
