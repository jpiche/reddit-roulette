package com.jpiche.redditroulette.activities

import android.app.Activity
import com.jpiche.redditroulette.BaseAct
import android.os.Bundle
import android.util.Log
import com.jpiche.redditroulette.fragments.SubredditListFragment

final class SubredditListActivity extends Activity with BaseAct {

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
}
