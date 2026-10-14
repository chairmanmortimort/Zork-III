package com.thelightphone.zork3

import android.util.Base64

/** Zork III story file loaded from assets at runtime via lightContext.readAsset(). */
object StoryData {
    /** Story bytes loaded from assets/story/zork3.z3. */
    val bytes: ByteArray
        get() = throw UnsupportedOperationException(
            "StoryData.bytes not embedded — load from assets/story/zork3.z3"
        )
}
