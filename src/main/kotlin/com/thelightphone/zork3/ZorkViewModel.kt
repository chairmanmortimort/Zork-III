package com.thelightphone.zork3

import com.thelightphone.sdk.LightViewModel
import com.thelightphone.zork3.engine.ZEvent
import com.thelightphone.zork3.engine.ZMachine
import com.thelightphone.zork3.engine.ZOutputBuffer
import com.thelightphone.zork3.engine.ZStory
import com.thelightphone.zork3.engine.ZStoryLoader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope

/** View model managing the Z-machine interpreter and save-system state. */
class ZorkViewModel : LightViewModel<Unit>() {

    companion object {
        private const val NUM_SLOTS = 3
    }

    private val output = ZOutputBuffer()
    private var machine: ZMachine? = null

    private val _lines = mutableListOf<String>()
    private val _text = MutableStateFlow("")
    val text: kotlinx.coroutines.flow.StateFlow<String> = _text.asStateFlow()

    private val _waiting = MutableStateFlow(false)
    val isWaiting: kotlinx.coroutines.flow.StateFlow<Boolean> = _waiting.asStateFlow()

    private val _quit = MutableStateFlow(false)
    val hasQuit: kotlinx.coroutines.flow.StateFlow<Boolean> = _quit.asStateFlow()

    // Trigger a recomposition on the saves screen after a save/delete completes.
    private val _saveRefresh = MutableStateFlow(Unit)
    val saveRefresh: kotlinx.coroutines.flow.StateFlow<Unit> = _saveRefresh.asStateFlow()

    /** Information about a single save slot for the saves screen to display. */
    data class SaveInfo(
        val slot: Int,
        val exists: Boolean,
        val sizeBytes: Long = 0L,
        val lastModified: Long = 0L,
    )

    // ── Save slot constants ──────────────────────────────────────────────
    private fun slotFile(filesDir: File, slot: Int) = filesDir.resolve("zork3_save_${slot}.bin")

    // ── Asset reader (set by the host screen before startGame) ────────────
    private var _assetReader: ((String) -> ByteArray)? = null
    fun setAssetReader(reader: (String) -> ByteArray) { _assetReader = reader }
    private fun readAsset(path: String): ByteArray =
        _assetReader?.invoke(path) ?: throw IllegalStateException(
            "asset reader not set — call setAssetReader before startGame"
        )

    // ── Game lifecycle ───────────────────────────────────────────────────
    /** Starts or resumes the game. On first launch tries to restore slot 1. */
    fun startGame(readAsset: (String) -> ByteArray, filesDir: File?) {
        _assetReader = readAsset
        _lines.clear()
        _waiting.value = false
        _quit.value = false

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val storyBytes = readAsset("story/zork3.z3")
                    val story = ZStoryLoader.fromBytes(storyBytes)

                    val fd = filesDir
                    if (fd != null) {
                        val slot1 = slotFile(fd, 1)
                        if (slot1.exists()) {
                            val saved = slot1.readBytes()
                            val m = ZMachine(story, output)
                            wireSaveCallbacks(m, fd)
                            val ok = m.restoreState(saved)
                            if (ok && !m.isQuit()) {
                                machine = m
                                collectEvents(m.resume())
                            } else {
                                slot1.delete()
                                startFresh(story, fd)
                            }
                        } else {
                            startFresh(story, fd)
                        }
                    } else {
                        startFresh(story, null)
                    }
                } catch (e: Exception) {
                    System.err.println("ZORK_DEBUG startGame ERROR: ${e.message}")
                    e.printStackTrace()
                    try {
                        val storyBytes = readAsset("story/zork3.z3")
                        val story = ZStoryLoader.fromBytes(storyBytes)
                        startFresh(story, filesDir)
                    } catch (e2: Exception) {
                        System.err.println("ZORK_DEBUG startGame ERROR2: ${e2.message}")
                        collectEvents(listOf(ZEvent.Text("Error loading story: ${e2.message}")))
                    }
                }
            }
        }
    }

    private fun startFresh(story: ZStory, filesDir: File?) {
        val m = ZMachine(story, output)
        if (filesDir != null) wireSaveCallbacks(m, filesDir)
        machine = m
        collectEvents(m.start())
    }

    private fun wireSaveCallbacks(m: ZMachine, filesDir: File) {
        m.onSaveRequest = { m.saveState().also { slotFile(filesDir, 1).writeBytes(it) } }
        m.onRestoreRequest = { slotFile(filesDir, 1).takeIf { it.exists() }?.readBytes() }
    }

    /** Submits a line of text input. */
    fun submitInput(text: String) {
        val m = machine ?: return
        if (!_waiting.value) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                collectEvents(m.submitInput(text))
            }
        }
    }

    // ── Save / Load / Delete slot helpers ────────────────────────────────

    /** Save the current game state to a specific slot (called from the saves screen). */
    fun saveToSlot(slot: Int, filesDir: File) {
        val m = machine ?: return
        if (_quit.value) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                slotFile(filesDir, slot).writeBytes(m.saveState())
                _saveRefresh.value = Unit
            }
        }
    }

    /** Load a specific slot and replace the current game state (called from resultCallback). */
    fun loadSlot(slot: Int, filesDir: File) {
        val f = slotFile(filesDir, slot)
        if (!f.exists()) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val storyBytes = readAsset("story/zork3.z3")
                    val story = ZStoryLoader.fromBytes(storyBytes)
                    val saved = f.readBytes()
                    val m = ZMachine(story, output)
                    wireSaveCallbacks(m, filesDir)
                    val ok = m.restoreState(saved)
                    if (ok && !m.isQuit()) {
                        machine = m
                        _lines.clear()
                        collectEvents(m.resume())
                    } else {
                        f.delete()
                        startFresh(story, filesDir)
                    }
                } catch (e: Exception) {
                    System.err.println("ZORK_DEBUG loadSlot ERROR slot=$slot: ${e.message}")
                    e.printStackTrace()
                }
                _saveRefresh.value = Unit
            }
        }
    }

    /** Delete a slot file (called from the saves screen). */
    fun deleteSlot(slot: Int, filesDir: File) {
        slotFile(filesDir, slot).delete()
        _saveRefresh.value = Unit
    }

    /** Return metadata for every slot so the saves screen can render the list. */
    fun listSaves(filesDir: File): List<SaveInfo> {
        return (1..NUM_SLOTS).map { slot ->
            val f = slotFile(filesDir, slot)
            SaveInfo(
                slot = slot,
                exists = f.exists(),
                sizeBytes = if (f.exists()) f.length() else 0L,
                lastModified = if (f.exists()) f.lastModified() else 0L,
            )
        }
    }

    /** Start a brand-new game, discarding the autosave. Called when user picks "New Game". */
    fun newGame(filesDir: File?) {
        if (filesDir != null) slotFile(filesDir, 1).delete()
        _lines.clear()
        _waiting.value = false
        _quit.value = false
        _saveRefresh.value = Unit
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val storyBytes = readAsset("story/zork3.z3")
                    val story = ZStoryLoader.fromBytes(storyBytes)
                    startFresh(story, filesDir)
                } catch (e: Exception) {
                    System.err.println("ZORK_DEBUG newGame ERROR: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    // ── Event collection ──────────────────────────────────────────────────

    private fun collectEvents(events: List<ZEvent>) {
        for (e in events) {
            when (e) {
                is ZEvent.Text -> _lines.add(e.text)
                ZEvent.Newline -> _lines.add("")
                ZEvent.InputPrompt -> _waiting.value = true
                ZEvent.Quit -> { _quit.value = true; _waiting.value = false }
            }
        }
        _text.value = _lines.joinToString("\n")
    }

    // ── Autosave hooks (called by the screen on pause / destroy) ─────────

    /**
     * Save the current state to the autosave slot (slot 1).
     * SYNCHRONOUS — safe to call on pause/destroy without coroutine scope.
     * Writes are small (~64KB) and complete in milliseconds.
     */
    fun saveGame(filesDir: File?) {
        val m = machine ?: return
        if (filesDir == null) return
        if (_quit.value) {
            slotFile(filesDir, 1).delete()
            return
        }
        slotFile(filesDir, 1).writeBytes(m.saveState())
    }
}
