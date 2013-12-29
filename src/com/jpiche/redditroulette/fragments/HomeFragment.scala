package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import android.app.Fragment
import android.view._
import android.os.Bundle
import android.view.View.OnClickListener
import com.jpiche.redditroulette.{TR, R}

case class HomeFragment() extends Fragment with OnClickListener {

  // this is a var instead of a case class argument because Android requires
  // a public empty constructor
  var listener: Option[HomeFragment.Listener] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)
    setHasOptionsMenu(true)
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    getActivity.getActionBar.setTitle(R.string.app_name)
    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_home, container, attachToRoot)

    val goBtn = v.findView(TR.go_btn)
    goBtn setOnClickListener this

    v
  }

  override def onClick(v: View) {
    v.getId match {
      case TR.go_btn.id => listener map { _.clickedGo() }
    }
    return
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.main, menu)
    super.onCreateOptionsMenu(menu, inflater)
  }
}

object HomeFragment {
  val FRAG_TAG = this.getClass.getSimpleName

  def apply(listener: Option[HomeFragment.Listener]) = {
    val h = new HomeFragment()
    h.listener = listener
    h
  }

  trait Listener {
    def clickedGo(): Unit
  }
}
