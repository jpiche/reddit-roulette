package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.Bundle
import android.webkit.{WebChromeClient, WebViewClient}
import com.jpiche.redditroulette.TR
import com.jpiche.redditroulette.reddit.Thing

case class WebFragment() extends ThingFragment {

  var listener: Option[WebFragment.Listener] = None

  private lazy val LOG_TAG = this.getClass.getSimpleName
  private lazy val webViewClient = new WebViewClient
  private lazy val webChromeClient = new WebChromeClient

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    if (thingUrl.isEmpty) {
      listener map { _.onError() }
      return null
    }

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_web, container, attachToRoot)
    val web = v.findView(TR.web)
    web.getSettings.setJavaScriptEnabled(true)
    web.getSettings.setLoadWithOverviewMode(true)
    web.getSettings.setBuiltInZoomControls(true)
    web.setWebViewClient(webViewClient)
    web.setWebChromeClient(webChromeClient)

    web loadUrl {
      if (savedInstanceState == null) thingUrl.get
      else savedInstanceState.getString(ThingFragment.URL_KEY, thingUrl.get)
    }

    v
  }

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val web = getView.findView(TR.web)
    outState.putString(ThingFragment.URL_KEY, web.getUrl)
  }

  def webView = getView.findView(TR.web)
}

object WebFragment {
  val FRAG_TAG = this.getClass.getSimpleName

  def apply(listener: Option[Listener], thing: Thing): WebFragment = {
    val frag = new WebFragment()
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