package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.Bundle
import android.webkit.{WebView, WebChromeClient, WebViewClient}
import com.jpiche.redditroulette.{FragTag, TR}
import com.jpiche.redditroulette.reddit.Thing
import android.util.Log

final case class WebFragment() extends ThingFragment {

  private lazy val webViewClient = new WebViewClient
  private lazy val webChromeClient = new WebChromeClient {
    override def onProgressChanged(view: WebView, prog: Int) {
      if (prog < 100)
        listener map { _ onProgress prog }
      else
        listener map { _.onFinished() }
      return
    }
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    if (thing.isEmpty) {
      Log.w(LOG_TAG, "Thing is empty")
      listener map { _.onError(thing) }
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
      if (savedInstanceState == null) thing.get.url
      else savedInstanceState.getString(WebFragment.URL_KEY, thing.get.url)
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

object WebFragment extends FragTag {
  private val URL_KEY = "URL_KEY"

  def apply(listener: Option[ThingListener], thing: Thing): WebFragment = {
    val frag = new WebFragment()
    frag.listener = listener
    frag.setArguments(thing.toBundle)
    frag
  }
}