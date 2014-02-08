package com.jpiche.redditroulette.activities

import android.app.{Fragment, Activity}
import com.jpiche.redditroulette.{R, BaseAct}
import android.os.Bundle
import android.util.Log
import com.jpiche.redditroulette.fragments.{SubredditAddDialogFragment, SubredditAddListener, SubredditListFragment}
import com.jpiche.redditroulette.reddit.Subreddit
import scala.util.Success
import scala.concurrent.{Future, promise}
import scala.concurrent.ExecutionContext.Implicits.global

import com.google.analytics.tracking.android.EasyTracker


final class SubredditListActivity extends Activity with BaseAct {

  private var listFrag: Option[SubredditListFragment] = None

  private lazy val addListener = new SubredditAddListener {
    def addSubreddit(name: CharSequence): Future[Boolean] = {
      val p = promise[Boolean]()

      val f = Subreddit.retrieve(name)
      f onComplete {
        case Success(Some(sub)) =>
          db.add(sub)
          p success true

        case _ =>
          toast(R.string.sub_error_retreive)
          p success false
      }
      p.future
    }

    def dismiss() {
      listFrag map { _.notifyChange() }
      return
    }
  }

  override def onStart() {
    super.onStart()

    EasyTracker.getInstance(this).activityStart(this)
  }

  override def onStop() {
    super.onStop()

    EasyTracker.getInstance(this).activityStop(this)
  }

  override def onCreate(inst: Bundle) {
    super.onCreate(inst)

    if (inst == null) {
      val frag = SubredditListFragment()
      val t = manager.beginTransaction()
      t.add(android.R.id.content, frag, SubredditListFragment.FRAG_TAG)
      t.commit()
    }

    getActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onNavigateUp(): Boolean = {
    finish()
    true
  }

  override def onAttachFragment(frag: Fragment) {
    frag match {
      case s@SubredditAddDialogFragment() => s.listener = Some(addListener)
      case l@SubredditListFragment() => listFrag = Some(l)
      case _ => return
    }
  }
}
