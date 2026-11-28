package everybody.codes.plugin

import kotlin.test.Test
import kotlin.test.assertEquals

class VerdictTest {
    private fun report(response: String) = json.decodeFromString<Verdict>(response).formatResult()

    @Test
    fun `no hints are required for either outcome`() {
        assertEquals("correct!", report("""{"correct":true}"""))
        assertEquals("WRONG ANSWER", report("""{"correct":false}"""))
    }

    @Test
    fun `only supplied hints are reported including false hints`() {
        assertEquals("WRONG ANSWER\nlength wrong", report("""{"correct":false,"lengthCorrect":false}"""))
        assertEquals(
            "WRONG ANSWER\nfirst character correct",
            report("""{"correct":false,"lengthCorrect":null,"firstCorrect":true}""")
        )
        assertEquals(
            "WRONG ANSWER\nlength correct, first character wrong",
            report("""{"correct":false,"lengthCorrect":true,"firstCorrect":false}""")
        )
    }

    @Test
    fun `cooldown prefers remaining time and falls back to the server deadline`() {
        assertEquals(
            "WRONG ANSWER\nRetry in 2s.",
            report("""{"correct":false,"penaltyLeftMs":2000,"penaltyUntil":12345}""")
        )
        assertEquals(
            "WRONG ANSWER\nCooldown until: 12345 (server timestamp)",
            report("""{"correct":false,"penaltyUntil":12345}""")
        )
        assertEquals("WRONG ANSWER", report("""{"correct":false,"penaltyLeftMs":0,"penaltyUntil":12345}"""))
    }
}
