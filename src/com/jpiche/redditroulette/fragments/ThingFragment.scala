package com.jpiche.redditroulette.fragments

import android.view.{MenuInflater, MenuItem, Menu}
import com.jpiche.redditroulette.{BaseFrag, R}
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import com.jpiche.redditroulette.reddit.Thing
import android.app.Fragment

abstract class ThingFragment extends Fragment with BaseFrag with PagerFrag {

  var thing: Option[Thing] = None
  var listener: Option[ThingListener] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      thing = Thing(args)
    }

    setHasOptionsMenu(true)
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.thing, menu)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    item.getItemId match {
      case R.id.external if thing.nonEmpty =>
        val i = new Intent(Intent.ACTION_VIEW, Uri.parse(thing.get.url))
        startActivity(i)
        true

      case R.id.info if thing.nonEmpty =>
        val dialog = ThingInfoDialogFragment(thing.get)
        dialog.show(getFragmentManager, ThingInfoDialogFragment.FRAG_TAG)
        true

      case R.id.next =>
        listener map { _.onNext(position) }
        true

      case R.id.save =>
        listener map { l =>
          thing map { t =>
            l.saveThing(t)
          }
        }
        true

      case _ => super.onOptionsItemSelected(item)
    }
}

object ThingFragment {
  final val KEY_POSITION = "__POSITION"
}

trait ThingListener {
  def onError(position: Int, thing: Option[Thing]): Unit
  def onNext(position: Int): Unit
  def saveThing(thing: Thing): Unit
}