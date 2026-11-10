package com.thelightphone.zork3

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

@EntryPoint
object Zork3EntryPoint : LightEntryPoint {
    // Zork III is fully offline — no server data or push notifications.
    override suspend fun onToolCreate(
        serverData: StateFlow<LightServerData?>,
    ) {
    }

    override suspend fun onPushNotification(
        data: ByteArray,
    ) {
    }
}
