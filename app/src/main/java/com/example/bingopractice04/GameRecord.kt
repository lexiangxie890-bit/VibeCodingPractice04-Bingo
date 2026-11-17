package com.example.bingopractice04

/**
 * Represents one completed Bingo round stored in SQLite.
 * The five numbers are the numbers selected by the player for that round.
 */
data class GameRecord(
    val id: Long = 0,
    val username: String,
    val roundNumber: Int,
    val numbers: List<Int>,
    val systemTime: String
)
