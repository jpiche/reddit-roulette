package com.jpiche.redditroulette.fragments

import scalaz._, Scalaz._
import android.app.Fragment
import android.view.{MenuInflater, MenuItem, Menu}
import com.jpiche.redditroulette.R
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.util.Log

abstract class ThingFragment extends Fragment {

  protected var thingUrl: Option[String] = None

  private lazy val LOG_TAG = this.getClass.getSimpleName

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      val title = args.getString(ThingFragment.TITLE_KEY, getResources.getString(R.string.app_name))
      getActivity.getActionBar.setTitle(title)

      val url = args.getString(ThingFragment.URL_KEY)
      thingUrl = if (url == null) None else url.some
    }

    setHasOptionsMenu(true)
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.thing, menu)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    item.getItemId match {
      case R.id.info if thingUrl.nonEmpty =>
        Log.i(LOG_TAG, "info menu item")
        val i = new Intent(Intent.ACTION_VIEW, Uri.parse(thingUrl.get))
        startActivity(i)
        true
      case _ => super.onOptionsItemSelected(item)
    }
}

object ThingFragment {
  val URL_KEY = "URL_KEY"
  val TITLE_KEY = "TITLE_KEY"
}
