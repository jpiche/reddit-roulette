package com.jpiche.redditroulette.views

import android.content.Context
import android.view.WindowManager
import android.graphics.Point
import android.widget.FrameLayout


trait ExtendedFrameLayout extends FrameLayout {
  private def windowManager = getContext.getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager]
  private def size: Point = {
    val point = new Point
    windowManager.getDefaultDisplay.getSize(point)
    point
  }

  def getYFraction: Float = {
    val y = size.y
    if (y == 0)
      0f
    else
      getY / y
  }

  def setYFraction(yFraction: Float) {
    val y = size.y
    setY(if (y > 0) yFraction * y else 0)
  }

  def getXFraction: Float = {
    val x = size.x
    if (x == 0)
      0f
    else
      getY / x
  }

  def setXFraction(xFraction: Float) {
    val x = size.x
    setY(if (x > 0) xFraction * x else 0)
  }
}

object ExtendedFrameLayout {
  def apply(context: Context) = new FrameLayout(context) with ExtendedFrameLayout
}