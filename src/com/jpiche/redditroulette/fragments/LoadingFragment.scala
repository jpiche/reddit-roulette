package com.jpiche.redditroulette.fragments

import android.app.Fragment
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle

import com.jpiche.redditroulette.{R, TR, BaseFrag}
import com.jpiche.redditroulette.TypedResource._


final case class LoadingFragment() extends Fragment with BaseFrag with PagerFrag {

  var loadingText = R.string.loading

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
    if (savedInstanceState != null) {
      loadingText = savedInstanceState.getInt(LoadingFragment.KEY_LOAD_TEXT, loadingText)
    }

    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_loading, container, attachToRoot)

    val progLayout = v findView TR.progressLayout
    progLayout setVisibility View.VISIBLE

    val prog = v findView TR.progress
    prog.setMax(100)

    val load = v findView TR.loadingText
    load setText loadingText

    v
  }

  override def onSaveInstanceState(outState: Bundle) {
    outState.putInt(LoadingFragment.KEY_LOAD_TEXT, loadingText)
    super.onSaveInstanceState(outState)
  }

  def setLoadingText(resId: Int) {
    run {
      val view = getView
      if (view != null) {
        val load = view findView TR.loadingText
        load setText resId
      }
    }
  }

  def setProgress(x: Int) {
    run {
      val view = getView
      if (view != null) {
        val prog = view findView TR.progress
        prog.setProgress(x)
      }
    }
  }
}

object LoadingFragment {
  final val KEY_LOAD_TEXT = "KEY_LOAD_TEXT"

  def apply(position: Int): LoadingFragment = {
    val frag = new LoadingFragment()
    val b = new Bundle()
    b.putInt(ThingFragment.KEY_POSITION, position)
    frag.setArguments(b)
    frag
  }
}