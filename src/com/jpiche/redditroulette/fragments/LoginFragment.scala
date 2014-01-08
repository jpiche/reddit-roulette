package com.jpiche.redditroulette.fragments

import scalaz._, Scalaz._
import com.jpiche.redditroulette.TypedResource._
import android.app.Fragment
import com.jpiche.redditroulette.{RouletteApp, FragTag, TR, BaseFrag}
import android.webkit.{WebView, WebChromeClient, WebViewClient}
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle
import com.jpiche.redditroulette.reddit.Thing
import com.netaporter.uri.Uri.parse

class LoginFragment extends Fragment with BaseFrag {

  var listener: Option[LoginFragment.Listener] = None

  private lazy val webViewClient = new WebViewClient {
    override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
      val uri = parse("url")
      val should = (uri.host | "") == RouletteApp.REDDIT_REDIRECT_HOST
      if (should) {
        val code = uri.query.param("code")
        val state = uri.query.param("state")
        if (code.isDefined && state.isDefined) {
          listener map { _.onLoginRedirect(code.get, state.get) }
        }
        true
      } else false
    }
  }

  private lazy val webChromeClient = new WebChromeClient

  private var mUrl: Option[String] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      mUrl = args.getString(LoginFragment.URL_KEY).some
      getActivity.getActionBar.setTitle("Login to Reddit")
    }
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    if (mUrl.isEmpty) return null

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_web, container, attachToRoot)
    val web = v.findView(TR.web)
    web.getSettings.setJavaScriptEnabled(true)
    web.getSettings.setLoadWithOverviewMode(true)
    web.getSettings.setBuiltInZoomControls(true)
    web.setWebViewClient(webViewClient)
    web.setWebChromeClient(webChromeClient)

    web loadUrl {
      if (savedInstanceState == null) mUrl.get
      else savedInstanceState.getString(LoginFragment.URL_KEY, mUrl.get)
    }

    v
  }

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val web = getView.findView(TR.web)
    outState.putString(Thing.KEY_URL, web.getUrl)
  }

  def webView = getView.findView(TR.web)
}

object LoginFragment extends FragTag {
  private val URL_KEY = "URL_KEY"

  def apply(url: String): LoginFragment = {
    val frag = new LoginFragment()
    val b = new Bundle()
    b.putString(URL_KEY, url)
    frag.setArguments(b)
    frag
  }

  trait Listener {
    def onLoginRedirect(code: String, state: String): Unit
  }
}