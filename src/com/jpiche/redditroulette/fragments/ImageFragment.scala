package com.jpiche.redditroulette.fragments

import android.view.{LayoutInflater, ViewGroup, View}
import android.os.Bundle
import android.util.Log
import android.graphics.Bitmap

import com.jpiche.redditroulette.TypedResource._
import com.jpiche.redditroulette.{TR, FragTag, LogTag}
import com.jpiche.redditroulette.reddit.Thing
import com.jpiche.redditroulette.net.WebData


final case class ImageFragment() extends ThingFragment {

  private var bitmap: Option[Bitmap] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      if (args.containsKey(ImageFragment.KEY_BITMAP)) {
        val bmp = args.getParcelable[Bitmap](ImageFragment.KEY_BITMAP)
        bitmap = Some(bmp)
      }

      position = args.getInt(ThingFragment.KEY_POSITION, position)
    }
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    Log.d(LOG_TAG, s"onCreateView with thing $thing")

    if (thing.isEmpty || bitmap.isEmpty) {
      listener map { _.onError(position, thing) }
      return null
    }

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_image, container, attachToRoot)

    val img = v findView TR.imageView
    val prog = v findView TR.progressLayout
    prog setVisibility View.VISIBLE

    img setImageBitmap bitmap.get
    prog setVisibility View.GONE

    v
  }
}

object ImageFragment extends FragTag with LogTag {
  private final val KEY_BITMAP = "__BITMAP"

  def apply(position: Int, thing: Thing, web: WebData): ImageFragment = {

    // This `.get` is safe since the surrounding call is wrapped in a check
    val bmp = web.toBitmap.get
    val frag = new ImageFragment()

    val b = thing.toBundle
    b.putInt(ThingFragment.KEY_POSITION, position)
    b.putParcelable(KEY_BITMAP, bmp)

    frag.setArguments(b)
    frag
  }
}
