package com.gosgf.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "go_records.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE games (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_black TEXT,
                player_white TEXT,
                date TEXT,
                moves TEXT,
                result TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS games")
        onCreate(db)
    }

    fun addGameRecord(playerBlack: String, playerWhite: String, date: String, moves: String, result: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("player_black", playerBlack)
            put("player_white", playerWhite)
            put("date", date)
            put("moves", moves)
            put("result", result)
        }
        return db.insert("games", null, values)
    }

    fun getAllGames(): List<GameRecord> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM games", null)
        val games = mutableListOf<GameRecord>()

        cursor.use {
            while (it.moveToNext()) {
                games.add(GameRecord(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    playerBlack = it.getString(it.getColumnIndexOrThrow("player_black")),
                    playerWhite = it.getString(it.getColumnIndexOrThrow("player_white")),
                    date = it.getString(it.getColumnIndexOrThrow("date")),
                    moves = it.getString(it.getColumnIndexOrThrow("moves")),
                    gameResult = it.getString(it.getColumnIndexOrThrow("result"))
                ))
            }
        }
        return games
    }
}

data class GameRecord(
    val id: Long,
    val playerBlack: String,
    val playerWhite: String,
    val date: String,
    val moves: String,
    val gameResult: String
)