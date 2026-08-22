package com.thelightphone.zork3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for browsing and managing save slots 1-3.
 *
 * Shows each slot's status inline. Slot 1 is the autosave slot (loaded on
 * start, saved on pause/destroy). Tapping "Load" on a slot returns that
 * slot number via goBack(result) so the host screen can restore the game.
 * Tapping "Save" writes the current in-memory state to that slot.
 */
class ZorkSavesScreen(
    sealedActivity: SealedLightActivity,
    private val currentSlot: Int,
    private val slotList: List<ZorkViewModel.SaveInfo>,
    private val onSaveToSlot: (Int) -> Unit,
    val onDeleteSlot: (Int) -> Unit,
) : SimpleLightScreen<Int>(sealedActivity) {

    @Composable
    override fun Content() {
        val colors by LightThemeController.colors.collectAsState()
        LightTheme(colors = colors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
            ) {
                LightTopBar(
                    leftButton = com.thelightphone.sdk.ui.LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack(currentSlot) },
                    ),
                    center = LightTopBarCenter.Text("Saves"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(1f.gridUnitsAsDp()),
                ) {
                    items(slotList) { info ->
                        val bg = if (info.slot == currentSlot)
                            LightThemeTokens.colors.contentSecondary.copy(alpha = 0.12f)
                        else LightThemeTokens.colors.background
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .clickable {
                                    goBack(info.slot)
                                }
                                .padding(1f.gridUnitsAsDp()),
                        ) {
                            // Top row: slot number + status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LightText(
                                    text = "Slot ${info.slot}",
                                    variant = LightTextVariant.Subheading,
                                    modifier = Modifier.weight(1f),
                                    align = TextAlign.Start,
                                )
                                LightText(
                                    text = if (info.exists) "Saved" else "Empty",
                                    variant = LightTextVariant.Detail,
                                    modifier = Modifier,
                                    align = TextAlign.End,
                                )
                            }

                            // Second row: date + size (if occupied)
                            if (info.exists) {
                                val dateStr = if (info.lastModified > 0) {
                                    try {
                                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                            .format(Date(info.lastModified))
                                    } catch (_: Exception) { "" }
                                } else ""
                                LightText(
                                    text = dateStr,
                                    variant = LightTextVariant.Fine,
                                    modifier = Modifier.fillMaxWidth(),
                                    align = TextAlign.Start,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
