package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import scala.concurrent.ExecutionContext.Implicits.global

import com.jpiche.redditroulette._
import android.app.{AlertDialog, Fragment}
import android.view._
import android.os.Bundle
import android.widget.{BaseAdapter, Adapter, AdapterView, SimpleCursorAdapter}
import android.database.Cursor
import android.widget.AdapterView.OnItemClickListener
import android.content.DialogInterface.OnClickListener
import android.content.DialogInterface
import android.util.Log


final case class SubredditListFragment() extends Fragment with BaseFrag with OnItemClickListener {

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)
    setHasOptionsMenu(true)
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    val attachToRoot = false
    val v = inflater.inflate(TR.layout.fragment_subredditlist, container, attachToRoot)

    val list = v.findView(TR.list)

    db.listSubs onSuccess {
      case c: Cursor =>
        val adapter = new SimpleCursorAdapter(
          thisContext,
          android.R.layout.simple_list_item_1,
          c,
          Array(Db.subreddit.KEY_NAME),
          Array(android.R.id.text1),
          0
        )
        list.setAdapter(adapter)
    }

    v
  }

  override def onResume() {
    super.onResume()

    val list = getView.findView(TR.list)
    list.setOnItemClickListener(this)
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.subreddit_list, menu)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    item.getItemId match {
      case R.id.add =>
        Log.i(LOG_TAG, "Add btn")
        true

      case _ => super.onOptionsItemSelected(item)
    }

  override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
    Log.i(LOG_TAG, s"onItemClick: (position: $position, id: $id)")

    db.findSub(id) map { s =>
      val builder = new AlertDialog.Builder(thisContext)

      val msg = getResources.getString(R.string.sub_confirm_delete, s.name)
      builder.setMessage(msg)

      builder.setPositiveButton(R.string.sub_confirm_delete_btn, new OnClickListener {
        def onClick(dialog: DialogInterface, which: Int) {
          db.deleteSub(id) onComplete {
            case _ =>
              parent.getAdapter match {
                case a: BaseAdapter => a.notifyDataSetChanged()
                case _ =>
              }
              dialog.dismiss()
          }
          return
        }
      })

      builder.setNegativeButton(R.string.dialog_cancel, new OnClickListener {
        def onClick(dialog: DialogInterface, which: Int) {
          dialog.dismiss()
        }
      })
      builder.create().show()
    }
    return
  }
}

object SubredditListFragment extends FragTag {

}
