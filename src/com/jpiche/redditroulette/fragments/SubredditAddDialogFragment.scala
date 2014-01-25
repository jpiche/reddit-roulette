package com.jpiche.redditroulette.fragments

import android.app.DialogFragment
import com.jpiche.redditroulette.{FragTag, TR, R, BaseFrag}
import android.os.{Handler, Bundle}
import android.view.{View, ViewGroup, LayoutInflater}
import com.jpiche.redditroulette.TypedResource._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import android.content.DialogInterface

final case class SubredditAddDialogFragment() extends DialogFragment with BaseFrag {

  var listener: Option[SubredditAddListener] = None

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)
    setStyle(DialogFragment.STYLE_NO_TITLE, 0)
    setCancelable(false)
  }

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.dialog_sub_add, container, false)

    val prog = view findView TR.progressLayout

    val editName = view findView TR.editName
    val input = editName.getText

    val cancelBtn = view findView TR.cancelBtn
    cancelBtn.setOnClickListener(new View.OnClickListener {
      def onClick(v: View) {
        dismiss()
      }
    })

    val handler = new Handler()

    val addBtn = view.findView(TR.addBtn)
    addBtn.setOnClickListener(new View.OnClickListener {
      def onClick(v: View) {
        if (input.length() < 1) {
          toast("Name is required.")
          return
        }

        if (listener.isEmpty) {
          toast("Error adding subreddit; please try again")
          dismiss()
        }

        handler.post(new Runnable {
          def run(): Unit = {
            prog setVisibility View.VISIBLE
            return
          }
        })

        val f = listener.get.addSubreddit(input)
        f onComplete {
          case Success(true) =>
            dismiss()
          case _ =>

            handler.post(new Runnable {
              def run(): Unit = {
                prog setVisibility View.GONE
                return
              }
            })
        }
        prefs.updatedSubreddits()
      }
    })

    view
  }

  override def onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)

    listener map { _.dismiss() }
    return
  }
}

object SubredditAddDialogFragment extends FragTag {

}

trait SubredditAddListener {
  def addSubreddit(name: CharSequence): Future[Boolean]
  def dismiss(): Unit
}
