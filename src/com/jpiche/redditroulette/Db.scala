package com.jpiche.redditroulette

import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._, Scalaz._

import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.content.{ContentValues, Context}
import com.jpiche.redditroulette.reddit.{Thing, Subreddit}
import org.joda.time.DateTime
import android.database.Cursor
import scala.concurrent.{Future, future}


sealed trait Db extends SQLiteOpenHelper {

  private def read[T](f: SQLiteDatabase => T): T = {
    val db = getReadableDatabase
    val t = f(db)
    if (db.isOpen) {
      db.close()
    }
    t
  }

  private def write[T](f: SQLiteDatabase => T): Future[T] = future {
    val db = getWritableDatabase
    val t = f(db)
    if (db.isOpen) {
      db.close()
    }
    t
  }

  override def onCreate(db: SQLiteDatabase) {
    db.execSQL(Db.subreddit.CREATE)
    db.execSQL(Db.things.CREATE)

    add(Subreddit.defaultSubs, db)
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(Db.subreddit.DROP)
    db.execSQL(Db.things.DROP)
    onCreate(db)
  }

  def add(thing: Thing) {
    val values = new ContentValues()
    values.put(Db.things.KEY_ID, thing.id)
    values.put(Db.things.KEY_VISITED, DateTime.now.getMillis.toDouble)
    values.put(Db.things.KEY_ISSELF, thing.isSelf)

    write { db =>
      db.insert(Db.things.TABLE, null, values)
    }
    return
  }

  def findThingVisited(id: String) = read { db =>
    val sql = s"${Db.things.SELECT} WHERE ${Db.things.KEY_ID} = ?"
    val cursor = db.rawQuery(sql, Array(id))
    val count = if (cursor.moveToFirst()) {
      cursor.getLong(0).some
    } else {
      None
    }
    cursor.close()
    count
  }

  def add(sub: Subreddit): Future[Unit] =
    add(Seq(sub))

  def add(subs: Seq[Subreddit]): Future[Unit] = write { db =>
    add(subs, db)
  }

  def add(subs: Seq[Subreddit], db: SQLiteDatabase): Unit = {
    db.beginTransaction()
    try {
      subs foreach { sub =>
        val values = new ContentValues()
        values.put(Db.subreddit.KEY_NAME, sub.name)
        values.put(Db.subreddit.KEY_NSFW, sub.nsfw)
        values.put(Db.subreddit.KEY_USE, sub.use)

        db.insert(Db.subreddit.TABLE, null, values)
      }

      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  def allSubs(nsfw: Boolean): Seq[Subreddit] = read { db =>
    val sql = if (nsfw) Db.subreddit.SELECT else Db.subreddit.SELECT_SFW
    val cursor = db.rawQuery(sql, null)

    val subs = if (cursor.moveToFirst())
      for {
        i <- 1 to cursor.getCount
      } yield {
        val s = Subreddit(
          name = cursor.getString(2),
          use = cursor.getInt(3) > 0,
          nsfw = cursor.getInt(4) > 0
        )
        cursor.moveToNext()
        s
      }
    else Nil

    cursor.close()
    subs
  }

  def countSubs: Int = read { db =>
    val cursor = db.rawQuery(Db.subreddit.COUNT, null)
    val count = if (cursor.moveToFirst()) {
      cursor.getInt(0)
    } else {
      0
    }
    cursor.close()
    count
  }

  def listSubs: Future[Cursor] = future {
    val db = getReadableDatabase
    db.rawQuery(Db.subreddit.SELECT, null)
  }

  def findSub(id: Long): Option[Subreddit] = read { db =>
    val sql = s"${Db.subreddit.SELECT} WHERE _id = $id LIMIT 1"
    val cursor = db.rawQuery(sql, null)

    if (cursor.moveToFirst()) {
      val s = Subreddit(
        name = cursor.getString(2),
        use = cursor.getInt(3) > 0,
        nsfw = cursor.getInt(4) > 0
      )
      cursor.close()
      s.some
    } else {
      None
    }
  }

  def deleteSub(id: Long): Future[Int] = write { db =>
    db.delete(Db.subreddit.TABLE, s"${Db.subreddit.KEY_PK} = $id", null)
  }
}

object Db extends DbTypes {
  val VERSION = 1
  val DATABASE_NAME = "reddit_roulette"

  def apply(context: Context): Db = new SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) with Db

  object subreddit extends dbTable {
    val TABLE = "subreddits"

    val KEY_PK = "_id"
    val KEY_ID = "id"
    val KEY_NAME = "name"
    val KEY_USE = "use"
    val KEY_NSFW = "nsfw"

    private val COLS = Map(
      KEY_PK -> TYPE_PK,
      KEY_ID -> TYPE_TEXT,
      KEY_NAME -> TYPE_TEXT,
      KEY_USE -> TYPE_INT,
      KEY_NSFW -> TYPE_INT
    )

    val SELECT = s"SELECT $KEY_PK, $KEY_ID, $KEY_NAME, $KEY_USE, $KEY_NSFW FROM $TABLE"
    val SELECT_SFW = s"$SELECT WHERE $KEY_NSFW = 0"
    val COUNT = s"SELECT COUNT($KEY_PK) FROM $TABLE LIMIT 1"
    val CREATE = s"CREATE TABLE $TABLE (${colsToString(COLS)})"
  }

  object things extends dbTable {
    val TABLE = "things"
    val KEY_PK = "_id"
    val KEY_ID = "id"
    val KEY_VISITED = "visited"
    val KEY_ISSELF = "is_self"

    private val COLS = Map(
      KEY_PK -> TYPE_PK,
      KEY_ID -> TYPE_TEXT,
      KEY_VISITED -> TYPE_INT,
      KEY_ISSELF -> TYPE_INT
    )

    val CREATE = s"CREATE TABLE $TABLE (${colsToString(COLS)})"
    val SELECT = s"SELECT $KEY_ID FROM $TABLE"
    val SELECT_NOSELF = s"$SELECT WHERE $KEY_ISSELF = 0"
  }
}

sealed trait DbTypes {
  val TYPE_PK = "INTEGER PRIMARY KEY"
  val TYPE_TEXT = "TEXT"
  val TYPE_INT = "INTEGER"
}

sealed trait dbTable {
  def TABLE: String
  def SELECT: String
  def CREATE: String

  lazy val DROP = s"DROP TABLE IF EXISTS $TABLE"

  def colsToString(cols: Map[String, String]): String = {
    val x = for {
      (c, t) <- cols
    } yield "%s %s" format (c, t)
    x mkString ", "
  }
}