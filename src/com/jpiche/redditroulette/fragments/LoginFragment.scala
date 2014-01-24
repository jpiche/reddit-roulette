package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.app.Fragment
import com.jpiche.redditroulette.{FragTag, TR, BaseFrag}
import android.webkit.{WebView, WebChromeClient, WebViewClient}
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle
import com.netaporter.uri.Uri.parse


final case class LoginFragment() extends Fragment with BaseFrag {

  private val REDIRECT_HOST = "redditroulette.jpiche.com"
  private val REDIRECT_PATH = "/app-redirect/"

  var listener: Option[LoginFragment.Listener] = None

  private val webViewClient = new WebViewClient {
    override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {

      val uri = parse(url)
      val params = uri.query.paramMap

      if (uri.host.isDefined
        && uri.host.get == REDIRECT_HOST
        && uri.path == REDIRECT_PATH
        && params.contains("code")
        && params.contains("state")
      ) {
        val code = uri.query.param("code")
        val state = uri.query.param("state")
        listener map {
          _.onLoginRedirect(code.get, state.get)
        }

        true
      } else {
        false
      }
    }
  }

  private val webChromeClient = new WebChromeClient

  private var mUrl: Option[String] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      mUrl = Some(args.getString(LoginFragment.URL_KEY))
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
    outState.putString(LoginFragment.URL_KEY, web.getUrl)
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