package com.jpiche.redditroulette.fragments

import com.jpiche.redditroulette.TypedResource._
import scala.concurrent.ExecutionContext.Implicits.global

import com.jpiche.redditroulette._
import android.app.{AlertDialog, Fragment}
import android.view._
import android.os.{Handler, Bundle}
import android.widget._
import android.widget.AdapterView.OnItemClickListener
import android.content.DialogInterface.OnClickListener
import android.content.DialogInterface
import android.util.Log
import scala.Some


final case class SubredditListFragment() extends Fragment with BaseFrag with OnItemClickListener {

  private var listAdapter: Option[SimpleCursorAdapter] = None

  private val handler = new Handler()

  def notifyChange() {
    handler.post(new Runnable {
      def run() {
        listAdapter map { a =>
          val c = db.listSubs
          a.changeCursor(c)
          val view = getView
          if (view != null) {
            val list = view findView TR.list
            list setAdapter a
            listAdapter = Some(a)
          }
        }
        return
      }
    })
    return
  }

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

    val c = db.listSubs
    val adapter = new SimpleCursorAdapter(
      thisContext,
      android.R.layout.simple_list_item_1,
      c,
      Array(Db.subreddit.KEY_NAME),
      Array(android.R.id.text1),
      0
    )
    listAdapter = Some(adapter)
    list.setAdapter(adapter)

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
        val s = SubredditAddDialogFragment()
        s.show(manager, SubredditAddDialogFragment.FRAG_TAG)
        true

      case R.id.revert =>
        val builder = new AlertDialog.Builder(thisContext)
        builder.setTitle(R.string.sub_revert)
        builder.setMessage(R.string.sub_revert_message)
        builder.setPositiveButton(R.string.sub_revert_confirm, new OnClickListener {
          def onClick(dialog: DialogInterface, which: Int) {
            db.revertSubreddits() onComplete {
              case _ =>
                notifyChange()
                dialog.dismiss()
            }
            prefs.updatedSubreddits()
          }
        })
        builder.setNegativeButton(R.string.dialog_cancel, new OnClickListener {
          def onClick(dialog: DialogInterface, which: Int) {
            dialog.dismiss()
          }
        })
        builder.create().show()

        true

      case _ => super.onOptionsItemSelected(item)
    }

  override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
    Log.i(LOG_TAG, s"onItemClick: (position: $position, id: $id)")

    db.findSub(id) map { s =>
      val builder = new AlertDialog.Builder(thisContext)

      val msg = getResources.getString(R.string.sub_confirm_delete, s.name)
      builder.setMessage(msg)
      builder.setTitle(R.string.sub_delete_title)
      builder.setPositiveButton(R.string.sub_confirm_delete_btn, new OnClickListener {
        def onClick(dialog: DialogInterface, which: Int) {
          db.deleteSub(id) onComplete {
            case _ =>
              prefs.updatedSubreddits()
              handler.post(new Runnable {
                def run() {
                  parent.getAdapter match {
                    case a: SimpleCursorAdapter =>
                      val c = db.listSubs
                      a.changeCursor(c)

                      parent match {
                        case p: ListView =>
                          p setAdapter a
                          listAdapter = Some(a)
                        case _ =>
                      }

                    case _ =>
                  }
                }
              })
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
