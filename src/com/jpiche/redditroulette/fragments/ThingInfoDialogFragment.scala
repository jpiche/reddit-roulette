package com.jpiche.redditroulette.fragments

import android.app.DialogFragment
import com.jpiche.redditroulette.{TR, R, FragTag, BaseFrag}
import android.os.Bundle
import com.jpiche.redditroulette.reddit.Thing
import android.content.Intent
import android.net.Uri
import android.view._
import com.jpiche.redditroulette.TypedResource._


final class ThingInfoDialogFragment extends DialogFragment with BaseFrag {

  private var mThing: Option[Thing] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)
    setStyle(DialogFragment.STYLE_NO_TITLE, 0)

    val args = getArguments
    if (args != null) {
      mThing = Thing(args)
    }
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    if (mThing.isEmpty) {
      return null
    }
    val thing = mThing.get

    val view = inflater.inflate(R.layout.dialog_info, container, false)
    val title = view.findView(TR.title)
    title.setText(thing.title)

    val info = view.findView(TR.post_info)
    info.setText(thing.info)

    val score = view.findView(TR.score)
    score.setText(thing.scoreInfo)

    val comments = view.findView(TR.comments)
    comments.setText(thing.comments.toString)

    val commentsBtn = view.findView(TR.open_comments)
    commentsBtn.setOnClickListener(new View.OnClickListener {
      def onClick(v: View) {
        val i = new Intent(Intent.ACTION_VIEW, Uri.parse(thing.full_permalink))
        startActivity(i)
        dismiss()
      }
    })

    val external = view.findView(TR.external)
    external.setOnClickListener(new View.OnClickListener {
      def onClick(v: View) {
        val i = new Intent(Intent.ACTION_VIEW, Uri.parse(thing.url))
        startActivity(i)
        dismiss()
      }
    })

    view
  }
}

object ThingInfoDialogFragment extends FragTag {

  def apply(thing: Thing) = {
    val frag = new ThingInfoDialogFragment
    frag.setArguments(thing.toBundle)
    frag
  }
}
