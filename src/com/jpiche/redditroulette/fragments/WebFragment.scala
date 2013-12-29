package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.app.Fragment
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.Bundle
import android.webkit.{WebChromeClient, WebViewClient}
import com.jpiche.redditroulette.TR
import com.jpiche.redditroulette.reddit.Thing

case class WebFragment() extends Fragment {

  var listener: Option[WebFragment.Listener] = None

  private lazy val LOG_TAG = this.getClass.getSimpleName
  private lazy val webViewClient = new WebViewClient
  private lazy val webChromeClient = new WebChromeClient

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args == null) {
      listener map { _.onError() }
    } else {
      val title = args.getString(WebFragment.TITLE_KEY)
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
    val url = args.getString(WebFragment.URL_KEY)

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_web, container, attachToRoot)
    val web = v.findView(TR.web)
    web.getSettings.setJavaScriptEnabled(true)
    web.getSettings.setLoadWithOverviewMode(true)
    web.getSettings.setBuiltInZoomControls(true)
    web.setWebViewClient(webViewClient)
    web.setWebChromeClient(webChromeClient)

    web loadUrl {
      if (savedInstanceState == null) url
      else savedInstanceState.getString(WebFragment.URL_KEY, url)
    }

    v
  }

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val web = getView.findView(TR.web)
    outState.putString(WebFragment.URL_KEY, web.getUrl)
  }

  def webView = getView.findView(TR.web)
}

object WebFragment {
  val FRAG_TAG = this.getClass.getSimpleName
  private val URL_KEY = "URL_KEY"
  private val TITLE_KEY = "TITLE_KEY"

  def apply(listener: Option[Listener], thing: Thing): WebFragment = {
    val frag = new WebFragment()
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