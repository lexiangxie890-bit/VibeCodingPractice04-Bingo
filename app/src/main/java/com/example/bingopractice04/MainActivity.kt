package com.example.bingopractice04

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bingopractice04.ui.theme.BingoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Identifies the three screens displayed by this single-activity application. */
private enum class AppScreen { LOGIN, GAME, HISTORY }

/**
 * Entry activity for Vibe Coding Practice 04.
 * It coordinates login, Bingo game logic, SQLite saving and history navigation.
 */
class MainActivity : ComponentActivity() {
    private lateinit var databaseHelper: BingoDatabaseHelper

    /** Initializes the SQLite helper and starts the Jetpack Compose interface. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseHelper = BingoDatabaseHelper(this)

        setContent {
            BingoTheme {
                var screen by rememberSaveable { mutableStateOf(AppScreen.LOGIN) }
                var username by rememberSaveable { mutableStateOf("") }
                var roundNumber by rememberSaveable { mutableIntStateOf(1) }
                var history by remember { mutableStateOf(emptyList<GameRecord>()) }

                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FB)) {
                    when (screen) {
                        AppScreen.LOGIN -> LoginScreen(
                            onLogin = { enteredUsername ->
                                username = enteredUsername
                                roundNumber = databaseHelper.getNextRoundNumber(username)
                                screen = AppScreen.GAME
                            }
                        )

                        AppScreen.GAME -> BingoGameScreen(
                            username = username,
                            roundNumber = roundNumber,
                            onRoundCompleted = { selectedNumbers, systemTime ->
                                databaseHelper.insertGameRecord(
                                    GameRecord(
                                        username = username,
                                        roundNumber = roundNumber,
                                        numbers = selectedNumbers,
                                        systemTime = systemTime
                                    )
                                )
                                roundNumber += 1
                            },
                            onShowHistory = {
                                history = databaseHelper.getGameRecords(username)
                                screen = AppScreen.HISTORY
                            },
                            onLogout = {
                                username = ""
                                screen = AppScreen.LOGIN
                            }
                        )

                        AppScreen.HISTORY -> HistoryScreen(
                            username = username,
                            records = history,
                            onBack = { screen = AppScreen.GAME }
                        )
                    }
                }
            }
        }
    }

    /** Closes the database connection when Android destroys the activity. */
    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}

/**
 * Displays username and password fields. For this classroom demonstration,
 * any non-empty values are accepted and the password is never stored.
 */
@Composable
private fun LoginScreen(onLogin: (String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("BINGO", fontSize = 38.sp, fontWeight = FontWeight.Black, color = Color(0xFF6750A4))
                Text(
                    "Sign in to start playing",
                    color = Color(0xFF667085),
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = "" },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = "" },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage.isNotEmpty()) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter both username and password."
                        } else {
                            onLogin(username.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp)
                ) { Text("Login") }
            }
        }
    }
}

/**
 * Displays a 1-25 number board. The user must select exactly five numbers.
 * Each round draws five random numbers, calculates matches and saves the record.
 */
@Composable
private fun BingoGameScreen(
    username: String,
    roundNumber: Int,
    onRoundCompleted: (List<Int>, String) -> Unit,
    onShowHistory: () -> Unit,
    onLogout: () -> Unit
) {
    val selectedNumbers = remember { mutableStateListOf<Int>() }
    var drawnNumbers by remember { mutableStateOf(emptyList<Int>()) }
    var resultMessage by remember { mutableStateOf("Select exactly five numbers.") }
    var roundSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Player: $username", color = Color(0xFF667085))
                Text("Round $roundNumber", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onLogout) { Text("Logout") }
        }

        Text(
            "Choose 5 numbers from 1 to 25",
            modifier = Modifier.padding(top = 22.dp, bottom = 12.dp),
            fontWeight = FontWeight.SemiBold
        )

        // Five rows of five circular number buttons form the Bingo selection board.
        for (row in 0 until 5) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (column in 1..5) {
                    val number = row * 5 + column
                    val isSelected = number in selectedNumbers
                    Button(
                        onClick = {
                            when {
                                isSelected -> selectedNumbers.remove(number)
                                selectedNumbers.size < 5 -> selectedNumbers.add(number)
                            }
                            drawnNumbers = emptyList()
                            resultMessage = "Selected ${selectedNumbers.size} of 5 numbers."
                            roundSaved = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF6750A4) else Color.White,
                            contentColor = if (isSelected) Color.White else Color(0xFF1D2939)
                        ),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) { Text(number.toString(), fontWeight = FontWeight.Bold) }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    selectedNumbers.clear()
                    selectedNumbers.addAll((1..25).shuffled().take(5).sorted())
                    drawnNumbers = emptyList()
                    resultMessage = "Five random player numbers selected."
                    roundSaved = false
                },
                modifier = Modifier.weight(1f)
            ) { Text("Random 5") }
            OutlinedButton(
                onClick = {
                    selectedNumbers.clear()
                    drawnNumbers = emptyList()
                    resultMessage = "Select exactly five numbers."
                    roundSaved = false
                },
                modifier = Modifier.weight(1f)
            ) { Text("Clear") }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Your numbers: ${selectedNumbers.sorted().joinToString().ifEmpty { "-" }}")
                Text(
                    "Drawn numbers: ${drawnNumbers.joinToString().ifEmpty { "-" }}",
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    resultMessage,
                    modifier = Modifier.padding(top = 10.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4527A0)
                )
            }
        }

        Button(
            onClick = {
                if (selectedNumbers.size != 5) {
                    resultMessage = "Please select exactly five numbers first."
                } else if (!roundSaved) {
                    drawnNumbers = (1..25).shuffled().take(5).sorted()
                    val matches = selectedNumbers.count { it in drawnNumbers }
                    resultMessage = if (matches == 5) {
                        "BINGO! All five numbers matched."
                    } else {
                        "$matches of 5 numbers matched. Round saved to SQLite."
                    }
                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    onRoundCompleted(selectedNumbers.sorted(), time)
                    roundSaved = true
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(52.dp)
        ) { Text(if (roundSaved) "Round Saved" else "Play Bingo") }

        OutlinedButton(
            onClick = onShowHistory,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp)
        ) { Text("View SQLite History") }
    }
}

/** Shows all SQLite game records for the logged-in user. */
@Composable
private fun HistoryScreen(
    username: String,
    records: List<GameRecord>,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Game History", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("SQLite records for $username", color = Color(0xFF667085))

        if (records.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No saved rounds yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Round ${record.roundNumber}", fontWeight = FontWeight.Bold)
                            Text("Five Bingo numbers: ${record.numbers.joinToString()}")
                            Text("System time: ${record.systemTime}", color = Color(0xFF667085))
                        }
                    }
                }
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Return to Game")
        }
    }
}
