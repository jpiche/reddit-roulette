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

  private trait ImgCallback extends Callback {
    def onSuccess() {
      handler.sendEmptyMessage(View.GONE)
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

    picasso.load(Uri.parse(thing.get.goodUrl))
      .into(img, new ImgCallback {
      def onError() {
        Log.w(LOG_TAG, "Error loading good url: %s" format thing.get.goodUrl)

        if (thing.get.goodUrl == thing.get.url) {
          listener map { _.onError(thing) }

        } else {
          Log.w(LOG_TAG, "Trying again with url from API: %s" format thing.get.url)
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

    v
  }
}

object ImageFragment {
  val FRAG_TAG = this.getClass.getSimpleName

  def apply(listener: Option[Listener], thing: Thing): ImageFragment = {
    val frag = new ImageFragment()
    frag.listener = listener
    frag.setArguments(thing.toBundle)
    frag
  }

  trait Listener {
    def onError(thing: Option[Thing]): Unit
  }
}
