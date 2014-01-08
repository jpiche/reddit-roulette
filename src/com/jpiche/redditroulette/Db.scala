package com.jpiche.redditroulette

import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.content.{ContentValues, Context}
import com.jpiche.redditroulette.reddit.{Thing, Subreddit}
import org.joda.time.DateTime


sealed trait Db extends SQLiteOpenHelper {

  override def onCreate(db: SQLiteDatabase) {
    db.execSQL(Db.subreddit.CREATE)
    db.execSQL(Db.things.CREATE)
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(Db.subreddit.DROP)
    db.execSQL(Db.things.DROP)
    onCreate(db)
  }

  def add(thing: Thing) {
    val db = getWritableDatabase

    val values = new ContentValues()
    values.put(Db.things.KEY_ID, thing.id)
    values.put(Db.things.KEY_VISITED, DateTime.now.getMillis.toDouble)
    values.put(Db.things.KEY_ISSELF, thing.isSelf)

    db.insert(Db.things.TABLE, null, values)
    db.close()
  }

  def add(sub: Subreddit) {
    add(Seq(sub))
  }

  def add(subs: Seq[Subreddit]) {
    val db = getWritableDatabase
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
      db.close()
    }
  }

  def allSubs(nsfw: Boolean): Seq[Subreddit] = {
    val db = getReadableDatabase
    val sql = if (nsfw) Db.subreddit.SELECT else Db.subreddit.SELECT_SFW
    val cursor = db.rawQuery(sql, null)

    val subs = if (cursor.moveToFirst())
      for {
        i <- 1 to cursor.getCount
      } yield {
        val s = Subreddit(
          name = cursor.getString(1),
          use = cursor.getInt(2) > 0,
          nsfw = cursor.getInt(3) > 0
        )
        cursor.moveToNext()
        s
      }
    else Nil

    cursor.close()
    subs
  }

  def countSubs: Int = {
    val db = getReadableDatabase
    val cursor = db.rawQuery(Db.subreddit.COUNT, null)
    val count = if (cursor.moveToFirst()) {
      cursor.getInt(0)
    } else {
      0
    }
    cursor.close()
    count
  }
}

object Db extends DbTypes {
  val VERSION = 1
  val DATABASE_NAME = "reddit_roulette"

  def apply(context: Context): Db = new SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) with Db

  object subreddit extends dbTable {
    val TABLE = "subreddits"

    val KEY_ID = "id"
    val KEY_NAME = "name"
    val KEY_USE = "use"
    val KEY_NSFW = "nsfw"

    private val COLS = Map(
      KEY_ID -> TYPE_PK,
      KEY_NAME -> TYPE_TEXT,
      KEY_USE -> TYPE_INT,
      KEY_NSFW -> TYPE_INT
    )

    val SELECT = "SELECT %s, %s, %s, %s FROM %s " format (KEY_ID, KEY_NAME, KEY_USE, KEY_NSFW, TABLE)
    val SELECT_SFW = "SELECT %s, %s, %s, %s FROM %s WHERE %s = 0" format (KEY_ID, KEY_NAME, KEY_USE, KEY_NSFW, TABLE, KEY_NSFW)
    val COUNT = "SELECT COUNT(%s) FROM %s " format (KEY_ID, TABLE)
    val CREATE = "CREATE TABLE %s (%s) " format (TABLE, colsToString(COLS))
  }

  object things extends dbTable {
    val TABLE = "things"
    val KEY_PK = "pk"
    val KEY_ID = "id"
    val KEY_VISITED = "visited"
    val KEY_ISSELF = "is_self"

    private val COLS = Map(
      KEY_PK -> TYPE_PK,
      KEY_ID -> TYPE_TEXT,
      KEY_VISITED -> TYPE_INT,
      KEY_ISSELF -> TYPE_INT
    )

    val CREATE = "CREATE TABLE %s (%s) " format (TABLE, colsToString(COLS))
    val SELECT = "SELECT %s FROM %s " format (KEY_ID, TABLE)
    val SELECT_NOSELF = SELECT + "WHERE %s = %d".format(KEY_ISSELF, 0)
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

  lazy val DROP = "DROP TABLE IF EXISTS %s" format TABLE

  def colsToString(cols: Map[String, String]): String = {
    val x = for {
      (c, t) <- cols
    } yield "%s %s" format (c, t)
    x mkString ", "
  }
}