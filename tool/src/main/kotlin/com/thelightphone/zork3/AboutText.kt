package com.thelightphone.zork3

/**
 * Static text content for the Zork III about screen.
 */
object AboutText {
    const val TITLE = "Zork III: The Dungeon Master"
    const val VERSION = "1.0.0"

    val CONTENT = """
        Zork III: The Dungeon Master

        You are the Dungeon Master, challenged to reach the Central Room of the Great
        Underground Empire and claim your title. Exploring a vast mythical world of
        perils and treasures, you must exhibit wisdom, courage, and endurance to
        succeed where others before you have failed.

        Zork III: The Dungeon Master was originally written by Tim Anderson, Marc
        Blank, Bruce Daniels, and Dave Lebling. Published by Infocom, Inc. in 1982.
        The original source code and Z-machine specification were released by Microsoft
        under the MIT License in 2025.

        This is an independent, non-commercial fan port for the Light Phone III.
        It uses the Z-machine interpreter (ZMachine3) by de.onyxbits.textfiction,
        distributed under the Apache License 2.0.
    """.trimIndent()
}
