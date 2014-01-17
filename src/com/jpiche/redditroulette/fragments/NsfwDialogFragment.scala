package com.jpiche.redditroulette.fragments

import android.app.{Dialog, AlertDialog, DialogFragment}
import android.os.Bundle
import com.jpiche.redditroulette._
import android.content.DialogInterface


final class NsfwDialogFragment extends DialogFragment with BaseFrag {

  override def onCreateDialog(inst: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(getActivity)
    builder.setTitle(R.string.pref_over18_title)
    builder.setMessage(R.string.pref_over19_alert_msg)
    builder.setPositiveButton(R.string.pref_over19_yes, new DialogInterface.OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int) {
        val b = prefs.edit().putBoolean(Prefs.PREF_NSFW, true).commit()
        if ( ! b) {
          toast(R.string.pref_write_error)
        }
      }
    })
    builder.setNegativeButton(R.string.pref_over19_no, new DialogInterface.OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int) {
        val b = prefs.edit().putBoolean(Prefs.PREF_NSFW, false).commit()
        if ( ! b) {
          toast(R.string.pref_write_error)
        }
      }
    })
    builder.create()
  }
}

object NsfwDialogFragment extends FragTag {
  def apply() = new NsfwDialogFragment
}