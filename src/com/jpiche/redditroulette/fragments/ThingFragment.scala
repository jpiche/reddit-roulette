package com.jpiche.redditroulette.fragments

import android.view.{MenuInflater, MenuItem, Menu}
import com.jpiche.redditroulette.{BaseFrag, R}
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import com.jpiche.redditroulette.reddit.Thing
import android.app.Fragment

abstract class ThingFragment extends Fragment with BaseFrag {

  protected var thing: Option[Thing] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      thing = Thing(args)
      thing map { t =>
        getActivity.getActionBar.setTitle(t.title)
      }
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

      case _ => super.onOptionsItemSelected(item)
    }
}
