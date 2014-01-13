package com.jpiche.redditroulette.fragments

import android.app.{AlertDialog, Dialog, DialogFragment}
import com.jpiche.redditroulette.{FragTag, R, BaseFrag}
import android.os.Bundle


final class AboutDialogFragment extends DialogFragment with BaseFrag {

  override def onCreateDialog(inst: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(getActivity)
    builder.setTitle(R.string.app_name)
    builder.setMessage(R.string.about_desc)
    builder.create()
  }
}

object AboutDialogFragment extends FragTag {
  def apply() = new AboutDialogFragment
}