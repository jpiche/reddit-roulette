package com.jpiche.redditroulette.fragments

import android.app.Fragment
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle

import com.jpiche.redditroulette.{R, TR, BaseFrag}
import com.jpiche.redditroulette.TypedResource._


final case class LoadingFragment() extends Fragment with BaseFrag with PagerFrag {

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
    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_loading, container, attachToRoot)

    val progLayout = v findView TR.progressLayout
    progLayout setVisibility View.VISIBLE

    val prog = v findView TR.progress
    prog.setMax(100)

    v
  }

  def setLoadingText(resId: Int) {
    run {
      val view = getView
      if (view != null) {
        val loadingText = view findView TR.loadingText
        loadingText.setText(resId)
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
  final val KEY_POSITION = "__POSITION"

  def apply(position: Int): LoadingFragment = {
    val frag = new LoadingFragment()
    val b = new Bundle()
    b.putInt(ThingFragment.KEY_POSITION, position)
    frag.setArguments(b)
    frag
  }
}