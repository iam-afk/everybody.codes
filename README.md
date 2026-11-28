# Everybody Codes

Kotlin solutions for [Everybody Codes](https://everybody.codes/). Each quest is a Gradle subproject under `events/<year>/<quest>/`. The examples below target [2026, quest 10](events/2026/10/10.kt); replace `2026` and `10` to select another quest.

## Setup

1. Use JDK 21 or newer and the included Gradle wrapper (`./gradlew` on macOS/Linux, `gradlew.bat` on Windows). Run the wrapper commands below from the repository root.
2. Sign in to Everybody Codes. In your browser’s developer tools, open cookie storage for the site and copy the **value** of the `everybody-codes` cookie.
3. Create `.everybody-codes` beside `settings.gradle.kts` and paste only that value on one line. Do not include quotes, `everybody-codes=`, or a `Cookie:` header. This file is ignored by Git; keep it private. On macOS/Linux, `chmod 600 .everybody-codes` restricts access to your user.
4. Make sure an HTTP proxy is listening on `127.0.0.1:8118`. The [plugin’s `ApiService`](gradle/everybody-codes-plugin/plugin/src/main/kotlin/everybody/codes/plugin/EverybodyCodesPlugin.kt) explicitly routes downloads and submissions through it. If you use a different proxy or connect directly, change or remove the `.proxy(...)` call there.

## Run a quest without submitting

```sh
./gradlew :events:2026:10:run
```

`run` fetches your personal notes and quest state as needed, decrypts the notes for each unlocked part, and calls its Kotlin solver. It prints each answer and its runtime without sending any answers. To inspect the input or result, look in `events/2026/10/build/everybody_codes/`: `notes1.txt` and `answer1.txt` are the files for part 1, with the same naming pattern for parts 2 and 3. Locked parts are skipped.

## Submit the next answer

```sh
./gradlew :events:2026:10:submit
```

**This sends a real answer to Everybody Codes.** You don’t need to run `run` first: `submit` runs the solver, checks fresh server state, and selects the first unanswered part in order (1, 2, 3). It submits at most one answer for that quest. If the selected part is locked, its answer file is missing, or all parts are solved, nothing is sent. A wrong answer can trigger a cooldown; check the printed verdict before retrying.

If you have a compatible `gradle` installed and your working directory is `events/2026/10/`, the short commands are `gradle run` and `gradle submit`. **Avoid an unqualified `submit` at the repository root:** Gradle can select the task in multiple quests. Use `./gradlew projects` to list registered quest paths.

## Add a quest

Create `events/<year>/<quest>/` and add `include("events:<year>:<quest>")` to [settings.gradle.kts](settings.gradle.kts), substituting the actual numbers. Put a `.kt` file directly in the new directory, without a package declaration. Define all three top-level functions: `part1(notes: String)`, `part2(notes: String)`, and `part3(notes: String)`. Each receives the full decrypted input and should **return** the answer, not just print it. The plugin generates `main`; no per-quest build script is needed.

## Solution commit messages

For the first version of a solution, use `<year>: quest <day>` as the entire commit message (for example, `2026: quest 16`).

For an optimized version, keep the same subject, leave a blank line, and add a short paragraph explaining what changed. For example:

```text
2026: quest 16

Count flip lengths without reversing tablets.
```
