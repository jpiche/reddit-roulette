import android.Keys._

android.Plugin.androidBuild

name := "RedditRoulette"

scalaVersion := "2.10.3"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-Ywarn-all",
  "-Ywarn-value-discard",
  "-Ywarn-numeric-widen",
  "-Ywarn-dead-code",
  "-Yinline-warnings",
  "-Xlint",
  "-Xfatal-warnings",
  "-language:implicitConversions"
)

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.5",
  "io.spray" %%  "spray-json" % "1.2.5",
  "com.squareup.okhttp" % "okhttp" % "1.2.1"
)

resolvers ++= Seq(
  "sbt-plugin-releases" at "http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/",
  "spray" at "http://repo.spray.io/"
)

useProguard in Android := true

platformTarget in Android := "android-19"

// call install and run without having to prefix with android:
run <<= run in Android

install <<= install in Android
