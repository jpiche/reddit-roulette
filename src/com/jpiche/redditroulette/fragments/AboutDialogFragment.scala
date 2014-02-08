package com.jpiche.redditroulette.fragments

import android.app.{AlertDialog, Dialog, DialogFragment}
import com.jpiche.redditroulette.{RouletteApp, FragTag, R, BaseFrag}
import android.os.Bundle
import android.content.{Intent, DialogInterface}
import android.content.DialogInterface.OnClickListener
import android.net.Uri


final class AboutDialogFragment extends DialogFragment with BaseFrag {

  override def onCreateDialog(inst: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(getActivity)
    builder.setTitle(R.string.app_name)
    builder.setMessage(R.string.about_desc)
    builder.setNegativeButton(R.string.dialog_close, new OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int) {
        dialog.dismiss()
      }
    })
    builder.setPositiveButton(R.string.about_like_btn, new OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int) {
        val intent = new Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse(RouletteApp.STORE_URL))
        startActivity(intent)
        dismiss()
      }
    })
    builder.create()
  }
}

object AboutDialogFragment extends FragTag {
  def apply() = new AboutDialogFragment
}