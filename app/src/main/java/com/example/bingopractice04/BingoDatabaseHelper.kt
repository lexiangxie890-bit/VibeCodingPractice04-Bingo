package com.example.bingopractice04

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Creates and manages the local SQLite database used by the Bingo game.
 * Each row stores the username, round number, five Bingo numbers and system time.
 */
class BingoDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    /** Creates the game_records table the first time the app opens the database. */
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(CREATE_GAME_RECORDS_TABLE)
    }

    /** Recreates the table if a future database version changes its structure. */
    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database.execSQL("DROP TABLE IF EXISTS $TABLE_GAME_RECORDS")
        onCreate(database)
    }

    /**
     * Inserts one completed game round and returns the new database row ID.
     * Exactly five sorted numbers must be supplied by the game screen.
     */
    fun insertGameRecord(record: GameRecord): Long {
        require(record.numbers.size == 5) { "A Bingo record must contain five numbers." }

        val values = ContentValues().apply {
            put(COLUMN_USERNAME, record.username)
            put(COLUMN_ROUND_NUMBER, record.roundNumber)
            put(COLUMN_NUMBER_1, record.numbers[0])
            put(COLUMN_NUMBER_2, record.numbers[1])
            put(COLUMN_NUMBER_3, record.numbers[2])
            put(COLUMN_NUMBER_4, record.numbers[3])
            put(COLUMN_NUMBER_5, record.numbers[4])
            put(COLUMN_SYSTEM_TIME, record.systemTime)
        }
        return writableDatabase.insert(TABLE_GAME_RECORDS, null, values)
    }

    /**
     * Calculates the next round number for a user from their highest stored round.
     * A new user therefore starts from round 1.
     */
    fun getNextRoundNumber(username: String): Int {
        val query = "SELECT MAX($COLUMN_ROUND_NUMBER) FROM $TABLE_GAME_RECORDS " +
            "WHERE $COLUMN_USERNAME = ?"
        readableDatabase.rawQuery(query, arrayOf(username)).use { cursor ->
            return if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) + 1 else 1
        }
    }

    /** Reads all game records for one user, with the latest round shown first. */
    fun getGameRecords(username: String): List<GameRecord> {
        val records = mutableListOf<GameRecord>()
        val selection = "$COLUMN_USERNAME = ?"
        val orderBy = "$COLUMN_ROUND_NUMBER DESC"

        readableDatabase.query(
            TABLE_GAME_RECORDS,
            null,
            selection,
            arrayOf(username),
            null,
            null,
            orderBy
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val usernameIndex = cursor.getColumnIndexOrThrow(COLUMN_USERNAME)
            val roundIndex = cursor.getColumnIndexOrThrow(COLUMN_ROUND_NUMBER)
            val number1Index = cursor.getColumnIndexOrThrow(COLUMN_NUMBER_1)
            val number2Index = cursor.getColumnIndexOrThrow(COLUMN_NUMBER_2)
            val number3Index = cursor.getColumnIndexOrThrow(COLUMN_NUMBER_3)
            val number4Index = cursor.getColumnIndexOrThrow(COLUMN_NUMBER_4)
            val number5Index = cursor.getColumnIndexOrThrow(COLUMN_NUMBER_5)
            val timeIndex = cursor.getColumnIndexOrThrow(COLUMN_SYSTEM_TIME)

            while (cursor.moveToNext()) {
                records += GameRecord(
                    id = cursor.getLong(idIndex),
                    username = cursor.getString(usernameIndex),
                    roundNumber = cursor.getInt(roundIndex),
                    numbers = listOf(
                        cursor.getInt(number1Index),
                        cursor.getInt(number2Index),
                        cursor.getInt(number3Index),
                        cursor.getInt(number4Index),
                        cursor.getInt(number5Index)
                    ),
                    systemTime = cursor.getString(timeIndex)
                )
            }
        }
        return records
    }

    private companion object {
        const val DATABASE_NAME = "bingo_game.db"
        const val DATABASE_VERSION = 1

        const val TABLE_GAME_RECORDS = "game_records"
        const val COLUMN_ID = "id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_ROUND_NUMBER = "round_number"
        const val COLUMN_NUMBER_1 = "bingo_number_1"
        const val COLUMN_NUMBER_2 = "bingo_number_2"
        const val COLUMN_NUMBER_3 = "bingo_number_3"
        const val COLUMN_NUMBER_4 = "bingo_number_4"
        const val COLUMN_NUMBER_5 = "bingo_number_5"
        const val COLUMN_SYSTEM_TIME = "system_time"

        const val CREATE_GAME_RECORDS_TABLE = """
            CREATE TABLE $TABLE_GAME_RECORDS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT NOT NULL,
                $COLUMN_ROUND_NUMBER INTEGER NOT NULL,
                $COLUMN_NUMBER_1 INTEGER NOT NULL,
                $COLUMN_NUMBER_2 INTEGER NOT NULL,
                $COLUMN_NUMBER_3 INTEGER NOT NULL,
                $COLUMN_NUMBER_4 INTEGER NOT NULL,
                $COLUMN_NUMBER_5 INTEGER NOT NULL,
                $COLUMN_SYSTEM_TIME TEXT NOT NULL
            )
        """
    }
}
