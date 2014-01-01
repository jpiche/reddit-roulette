package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.{Message, Handler, Bundle}
import com.squareup.picasso.{Callback, Picasso}
import android.net.Uri
import android.util.Log
import com.jpiche.redditroulette.TR
import com.jpiche.redditroulette.reddit.Thing

final case class ImageFragment() extends ThingFragment {

  var listener: Option[ImageFragment.Listener] = None

  private lazy val LOG_TAG = this.getClass.getSimpleName
  private lazy val picasso = Picasso `with` getActivity

  private val handler = new Handler(new Handler.Callback {
    def handleMessage(msg: Message): Boolean = {
      val v = getView
      if (v == null) return false

      val prog = getView.findView(TR.progress)
      if (prog == null) return false

      prog.setVisibility(msg.what)
      false
    }
  })

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    if (thingUrl.isEmpty) {
      listener map { _.onError() }
      return null
    }

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_image, container, attachToRoot)
    val img = v.findView(TR.imageView)

    picasso.load(Uri.parse(thingUrl.get))
      .into(img, new Callback {
      def onError() {
        listener map { _.onError() }
        return
      }

      def onSuccess() {
        handler.sendEmptyMessage(View.GONE)
        return
      }
    })

    v
  }
}

object ImageFragment {
  val FRAG_TAG = this.getClass.getSimpleName

  def apply(listener: Option[Listener], thing: Thing): ImageFragment = {
    val frag = new ImageFragment()
    frag.listener = listener
    val b = new Bundle()
    b.putString(ThingFragment.URL_KEY, thing.url)
    b.putString(ThingFragment.TITLE_KEY, thing.title)
    frag.setArguments(b)
    frag
  }

  trait Listener {
    def onError(): Unit
  }
}