package com.jpiche.redditroulette.fragments

import android.app.Fragment
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle

import com.jpiche.redditroulette.{TR, BaseFrag}
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

    val prog = v findView TR.progressLayout
    prog setVisibility View.VISIBLE

    v
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