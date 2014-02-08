package com.jpiche.redditroulette.fragments

import android.view.{MenuInflater, MenuItem, Menu}
import com.jpiche.redditroulette.{BaseFrag, R}
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import com.jpiche.redditroulette.reddit.Thing
import android.app.Fragment
import android.graphics.Bitmap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


abstract class ThingFragment extends Fragment
    with BaseFrag
    with PagerFrag { self =>

  var thing: Option[Thing] = None
  var listener: Option[ThingListener] = None

  private var saved = false
  private var saveProcess = false

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    val args = getArguments
    if (args != null) {
      thing = Thing(args)
      thing map { t=>
        saved = t.saved
      }
    }

    setHasOptionsMenu(true)
  }

  override def onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    val saveItem = menu.findItem(R.id.save)
    val unsaveItem = menu.findItem(R.id.unsave)

    if (saveItem != null && unsaveItem != null) {
      saveItem setEnabled ! saveProcess
      unsaveItem setEnabled ! saveProcess

      if (saved) {
        saveItem setVisible false
        unsaveItem setVisible true
      } else {
        saveItem setVisible true
        unsaveItem setVisible false
      }
    }
    ()
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
            saveProcess = true
            manager.invalidateOptionsMenu()
            l.saveThing(t) andThen {
              case _ =>
                saveProcess = false
                manager.invalidateOptionsMenu()
            } onSuccess {
              case true =>
                saved = true
                manager.invalidateOptionsMenu()
              case false =>
            }
          }
        }
        true

      case R.id.unsave =>
        listener map { l =>
          thing map { t =>
            saveProcess = true
            manager.invalidateOptionsMenu()
            l.unsaveThing(t) andThen {
              case _ =>
                saveProcess = false
                manager.invalidateOptionsMenu()
            } onSuccess {
              case true =>
                saved = false
                manager.invalidateOptionsMenu()
              case false =>
            }
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
  def saveThing(thing: Thing): Future[Boolean]
  def unsaveThing(thing: Thing): Future[Boolean]
  def setImageAs(thing: Thing, data: Bitmap): Unit
}
