# Lecture 10 Vibe Coding Practice 04

This is a clean Android Studio project developed in Kotlin with Jetpack Compose.
It implements a playable Bingo game and stores every completed round in a local
SQLite database.

## Required features

### Game logic

- Login with a non-empty username and password
- Select exactly five unique numbers from 1 to 25
- Optional Random 5 and Clear controls
- Draw five unique system numbers for each round
- Count matching numbers and detect a five-number Bingo
- Automatically increase the round number after a saved game

### SQLite database

`BingoDatabaseHelper` extends Android `SQLiteOpenHelper`. The `game_records`
table contains all attributes requested in the classwork:

| Attribute | SQLite column |
| --- | --- |
| Username | `username` |
| Round number | `round_number` |
| Five Bingo numbers | `bingo_number_1` to `bingo_number_5` |
| System time | `system_time` |

The Game History screen reads the stored records back from SQLite so the
database implementation can be demonstrated during marking.

## Source files

- `MainActivity.kt` - login, screen navigation, Bingo logic and Compose UI
- `BingoDatabaseHelper.kt` - SQLite table creation, insert and query operations
- `GameRecord.kt` - model for one stored game round
- `Theme.kt` - Material 3 application theme

Each Kotlin source file and each important function contains explanatory
comments.

## How to run

1. Open this folder in Android Studio.
2. Wait for Gradle sync to finish.
3. Run the `app` configuration on an emulator or Android device.
4. Enter any non-empty username and password.
5. Select five numbers and tap **Play Bingo**.
6. Open **View SQLite History** to confirm the stored record.

## Clean GitHub submission

The `.gitignore` excludes local and generated files such as `.gradle`, `.idea`,
`local.properties`, and all `build` folders. Upload this project to GitHub and
submit the GitHub repository URL to Moodle.
