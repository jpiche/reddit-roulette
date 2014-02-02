package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.view.{LayoutInflater, ViewGroup, View}
import android.os.Bundle
import android.webkit.{WebView, WebChromeClient, WebViewClient}
import com.jpiche.redditroulette.{R, FragTag, TR}
import com.jpiche.redditroulette.reddit.Thing
import android.util.Log


final case class WebFragment() extends ThingFragment {
  import WebFragment.{apply => _, _}

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      position = args.getInt(ThingFragment.KEY_POSITION, position)
    }
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    if (thing.isEmpty) {
      warn("Thing is empty")
      listener map { _.onError(position, thing) }
      return null
    }

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_web, container, attachToRoot)

    val prog = v findView TR.progressLayout
    prog setVisibility View.VISIBLE

    val loadText = v findView TR.loadingText
    loadText setText R.string.loading_web

    val web = v.findView(TR.web)
    web.getSettings.setJavaScriptEnabled(true)
    web.getSettings.setLoadWithOverviewMode(true)
    web.getSettings.setBuiltInZoomControls(true)
    web.setWebViewClient(webViewClient)
    web.setWebChromeClient(webChromeClient)

    web loadUrl {
      if (savedInstanceState == null) thing.get.url
      else savedInstanceState.getString(URL_KEY, thing.get.url)
    }

    v
  }

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (getView != null) {
      val web = getView.findView(TR.web)
      outState.putString(URL_KEY, web.getUrl)
    }
  }

  def webView = getView.findView(TR.web)
}

object WebFragment extends FragTag {
  private final val URL_KEY = "URL_KEY"

  private lazy val webViewClient = new WebViewClient {
    override def onPageFinished(view: WebView, url: String) {
      view.getParent match {
        case x: ViewGroup =>
          val prog = x findView TR.progressLayout
          prog setVisibility View.GONE
        case _ =>
      }
    }
  }
  private lazy val webChromeClient = new WebChromeClient


  def apply(p: Int, thing: Thing): WebFragment = {
    val frag = new WebFragment()
    val b = thing.toBundle
    b.putInt(ThingFragment.KEY_POSITION, p)
    frag.setArguments(b)
    frag
  }
}