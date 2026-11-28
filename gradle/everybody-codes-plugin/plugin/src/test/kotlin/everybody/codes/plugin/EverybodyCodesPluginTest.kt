package everybody.codes.plugin

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EverybodyCodesPluginTest {
    @Test
    fun `plugin registers quest tasks and wires the downloads to each part`() {
        val event = ProjectBuilder.builder().withName("2026").build()
        val quest = ProjectBuilder.builder().withParent(event).withName("7").build()
        quest.pluginManager.apply("everybody.codes")

        val tasks = quest.tasks
        assertTrue(tasks.names.containsAll(listOf("generateMain", "run", "submit")))
        val notes = tasks.named("downloadNotes", DownloadNotes::class.java).get()
        val state = tasks.named("downloadState", DownloadState::class.java).get()
        assertEquals("2026", notes.event.get())
        assertEquals("7", notes.quest.get())
        assertEquals(notes.event.get(), state.event.get())
        assertEquals(notes.quest.get(), state.quest.get())

        for (part in 1..3) {
            val decrypt = tasks.named("decryptNotes$part", DecryptNotes::class.java).get()
            assertEquals(part.toString(), decrypt.part.get())
            assertEquals(notes.notesFile.get().asFile, decrypt.notesFile.get().asFile)
            assertEquals(state.stateFile.get().asFile, decrypt.statusFile.get().asFile)
        }
    }
}
