package com.jpiche.redditroulette

import scalaz._, Scalaz._

import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.content.Context

sealed trait FlingDirection
case object FlingTop extends FlingDirection
case object FlingRight extends FlingDirection
case object FlingLeft extends FlingDirection
case object FlingBottom extends FlingDirection

abstract class FlingListener(implicit val context: Context) extends OnTouchListener {

  def onFling(dir: FlingDirection): Unit

  private lazy val gestureDetector = new GestureDetector(context, new FlingGestureListener(onFling))

  final override def onTouch(v: View, event: MotionEvent): Boolean =
    gestureDetector onTouchEvent event
}

private final class FlingGestureListener(val f: (FlingDirection) => Unit) extends SimpleOnGestureListener {
  protected val SWIPE_THRESHOLD = 100
  protected val SWIPE_VELOCITY_THRESHOLD = 100

  override def onDown(event: MotionEvent) = true

  override def onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = {
    val diffY = e2.getY - e1.getY
    val diffX = e2.getX - e1.getX
    val dir: Option[FlingDirection] = if (Math.abs(diffX) > Math.abs(diffY)) {
      if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
        if (diffX > 0)
          FlingRight.some
        else
          FlingLeft.some
      } else {
        None
      }
    } else {
      if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
        if (diffY > 0)
          FlingBottom.some
        else
          FlingTop.some
      } else {
        None
      }
    }

    dir match {
      case Some(x) =>
        f(x)
        true
      case None =>
        false
    }
  }
}
