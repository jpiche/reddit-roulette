package com.jpiche.redditroulette.fragments

import android.app.{AlertDialog, Dialog, DialogFragment}
import com.jpiche.redditroulette.{FragTag, BaseFrag}
import android.os.Bundle
import com.jpiche.redditroulette.reddit.Thing
import android.util.Log


final class ThingInfoDialogFragment extends DialogFragment with BaseFrag {

  override def onCreateDialog(inst: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(thisContext)

    val args = getArguments
    if (args != null) {
      Thing(args) map { thing =>
        builder.setTitle(thing.r_sub)
        builder.setMessage(thing.title)
      }
    }

    builder.create()
  }
}

object ThingInfoDialogFragment extends FragTag {

  def apply(thing: Thing) = {
    val frag = new ThingInfoDialogFragment
    frag.setArguments(thing.toBundle)
    frag
  }
}
