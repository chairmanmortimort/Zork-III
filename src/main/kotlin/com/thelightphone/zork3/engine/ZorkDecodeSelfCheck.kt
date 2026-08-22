package com.thelightphone.zork3.engine

/**
 * Self-check: load zork3.z3, decode the print_ret string at pc=41430 using the
 * actual ZStory/ZText classes, and show the result vs. what real Zork text should look like.
 *
 * This verifies the abbreviation table fix (byte 64), the 17-entries/block formula,
 * AND surfaces any remaining decode bugs.
 */
object ZorkDecodeSelfCheck {

    @JvmStatic
    fun main(args: Array<String>) {
        val storyBytes = java.io.File("src/main/assets/story/zork3.z3").readBytes()
        val story = ZStoryLoader.fromBytes(storyBytes)

        println("=== Zork abbreviation decode self-check ===")
        println("Story: ${storyBytes.size} bytes, version ${story.version}")
        println()

        // ── Print_ret at pc=41430 ─────────────────────────────────────────────
        val pc = 41430
        val opcodeByte = story.readByte(pc)
        val packedAddr = story.readWord(pc + 1)
        val stringByteAddr = packedAddr * 2

        println("print_ret at pc=$pc:")
        println("  opcode byte at pc:     0x${opcodeByte.toString(16).padStart(2, '0')}")
        println("  packed addr at pc+1:   0x${packedAddr.toString(16).padStart(4, '0')}")
        println("  string byte addr:      $stringByteAddr (0x${stringByteAddr.toString(16).padStart(4, '0')})")
        println("  file size:             ${story.data.size}")
        println("  in range:              ${0 <= stringByteAddr && stringByteAddr + 2 <= story.data.size}")
        println()

        // ── Decode using the actual ZText.decode (with fixes) ─────────────────
        val decoded = ZText.decode(story, stringByteAddr)
        println("=== Decoded string (ZText.decode with fixes) ===")
        println("  Length: ${decoded.length}")
        println("  Content: ${decoded.take(400)}")
        println()

        // ── Zork indicators ────────────────────────────────────────────────────
        val indicators = listOf(
            "Zork", "Dungeon", "West", "House", "white", "ruins", "dark",
            "room", "street", "standing", "open", "field", "forest", "maze"
        )
        val found = indicators.filter { it in decoded }
        println("Zork indicators found: $found")
        if (found.isNotEmpty()) {
            println(">>> RESULT: LIKELY CORRECT Zork text <<<")
        } else {
            println(">>> RESULT: STILL GARBAGE — remaining decode bug <<<")
        }
        println()

        // ── Raw decode (no abbreviations) to see z-char structure ─────────────
        println("=== Raw z-char decode (first 150 chars, no abbrev expansion) ===")
        val raw = rawDecode(story, stringByteAddr, 150)
        println(raw)
        println()

        // ── First 5 abbreviated entries from the table ────────────────────────
        println("=== First 5 abbreviation entries (block 1, lowercase, bytes 64+) ===")
        for (i in 0 until 5) {
            val wordAddr = story.abbrevTable + i * 2
            val w = story.readWord(wordAddr)
            val byteAddr = w * 2
            val inRange = byteAddr > 0 && byteAddr < story.data.size
            print("  entry[$i]: packed=0x${w.toString(16).padStart(4, '0')} -> byte=$byteAddr (${if (inRange) "in-range" else "OUT OF RANGE"})")
            if (inRange) {
                val abText = ZText.decode(story, byteAddr)
                print(" -> decodes to: ${abText.take(60)}")
            }
            println()
        }
        println()

        // ── Verify entry formula for specific blocks ──────────────────────────
        println("=== Entry formula: (block-1)*16 + (z-1) ===")
        val testCases = listOf(
            Triple(1, 1, 0),    // block=1, z=1 -> entry 0
            Triple(1, 16, 15),  // block=1, z=16 -> entry 15
            Triple(2, 1, 16),   // block=2, z=1 -> entry 16
            Triple(2, 16, 31),  // block=2, z=16 -> entry 31
            Triple(3, 1, 32),   // block=3, z=1 -> entry 32
        )
        for (tc in testCases) {
            val (block, z, expectedEntry) = tc
            val entry = (block - 1) * 16 + (z - 1)
            val wordAddr = story.abbrevTable + entry * 2
            val w = story.readWord(wordAddr)
            val byteAddr = w * 2
            val ok = if (entry == expectedEntry) "OK" else "MISMATCH (expected $expectedEntry)"
            print("  block=$block z=$z: entry=$entry $ok -> word 0x${w.toString(16).padStart(4, '0')} -> byte $byteAddr")
            if (byteAddr > 0 && byteAddr < story.data.size) {
                val abText = ZText.decode(story, byteAddr)
                print(" -> '${abText.take(50)}'")
            }
            println()
        }
        println()

        // ── Decode the first abbreviation and show context ────────────────────
        println("=== Decode block=1, z=1 abbreviation in isolation ===")
        val entry0_word = story.readWord(story.abbrevTable)
        val entry0_byte = entry0_word * 2
        println("  entry[0] word: 0x${entry0_word.toString(16).padStart(4, '0')} -> byte $entry0_byte")
        if (entry0_byte > 0 && entry0_byte < story.data.size) {
            val abDecoded = ZText.decode(story, entry0_byte)
            println("  decoded: '${abDecoded.take(100)}'")
            println("  length: ${abDecoded.length}")
        }
        println()

        // ── Show the first 10 words of the print_ret string ───────────────────
        println("=== First 10 words of the print_ret string at byte $stringByteAddr ===")
        for (i in 0 until 10) {
            val w = story.readWord(stringByteAddr + i * 2)
            val z0 = (w shr 11) and 0x1F
            val z1 = (w shr 6) and 0x1F
            val z2 = (w shr 1) and 0x1F
            val end = (w and 0x8000) != 0
            print("  word[$i]: 0x${w.toString(16).padStart(4, '0')} end=$end z=[$z0,$z1,$z2] -> ")
            for (z in intArrayOf(z0, z1, z2)) {
                when {
                    z == 0 -> print("SP ")
                    z == 1 -> print("U ")
                    z == 2 -> print("P ")
                    z == 3 -> print("AB ")
                    z == 4 -> print("L2 ")
                    z == 5 -> print("L1 ")
                    else -> {
                        val idx = z - 6
                        if (idx in 0 until 26) print("'$' + $idx ")
                        else print("?($z) ")
                    }
                }
            }
            println()
        }
    }

    /** Raw decode without abbreviation expansion — shows the z-char structure. */
    private fun rawDecode(story: ZStory, textStart: Int, maxChars: Int): String {
        val sb = StringBuilder()
        var addr = textStart
        var count = 0
        while (count < maxChars && addr < story.data.size - 1) {
            val word = (story.readByte(addr) shl 8) or story.readByte(addr + 1)
            addr += 2
            val end = (word and 0x8000) != 0
            val z0 = (word shr 11) and 0x1F
            val z1 = (word shr 6) and 0x1F
            val z2 = (word shr 1) and 0x1F
            for (z in intArrayOf(z0, z1, z2)) {
                count++
                when {
                    z == 0 -> sb.append(' ')
                    z == 1 -> sb.append("[U]")
                    z == 2 -> sb.append("[P]")
                    z == 3 -> sb.append("[AB]")
                    z == 4 -> sb.append("[L2]")
                    z == 5 -> sb.append("[L1]")
                    else -> {
                        val idx = z - 6
                        sb.append(if (idx in 0 until 26) ('a' + idx).toString() else "?$z")
                    }
                }
            }
            if (end) { sb.append("(END)"); break }
        }
        return sb.toString()
    }
}
