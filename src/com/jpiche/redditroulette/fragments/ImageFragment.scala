package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.app.Fragment
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.{Message, Handler, Bundle}
import com.squareup.picasso.{Callback, Picasso}
import android.net.Uri
import android.util.Log
import com.jpiche.redditroulette.TR
import com.jpiche.redditroulette.reddit.Thing

case class ImageFragment() extends Fragment {

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

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args == null) {
      listener map { _.onError() }
    } else {
      val title = args.getString(ImageFragment.TITLE_KEY)
      getActivity.getActionBar.setTitle(title)
    }
    return
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    val args = getArguments
    if (args == null) {
      listener map { _.onError() }
      return null
    }
    val url = args.getString(ImageFragment.URL_KEY)

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_image, container, attachToRoot)
    val img = v.findView(TR.imageView)

    picasso.load(Uri.parse(url))
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
  private val URL_KEY = "URL_KEY"
  private val TITLE_KEY = "TITLE_KEY"

  def apply(listener: Option[Listener], thing: Thing): ImageFragment = {
    val frag = new ImageFragment()
    frag.listener = listener
    val b = new Bundle()
    b.putString(URL_KEY, thing.url)
    b.putString(TITLE_KEY, thing.title)
    frag.setArguments(b)
    frag
  }

  trait Listener {
    def onError(): Unit
  }
}