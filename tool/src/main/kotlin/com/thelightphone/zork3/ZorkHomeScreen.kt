package com.thelightphone.zork3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Zork III for LightOS — a calm, monochrome text adventure. */
@InitialScreen
class ZorkHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, ZorkViewModel>(sealedActivity) {

    override val viewModelClass: Class<ZorkViewModel>
        get() = ZorkViewModel::class.java

    override fun createViewModel(): ZorkViewModel {
        val storyBytes = lightContext.readAsset("story/zork3.z3")
        return ZorkViewModel(lightContext.filesDir, storyBytes)
    }

    private fun openSaves() {
        navigateTo(
            screenFactory = { sa -> ZorkSavesScreen(sealedActivity = sa, slots = viewModel.listSlots(), onDelete = { viewModel.deleteSlot(it) }) },
            resultCallback = { resultSlot: Int? ->
                if (resultSlot != null && resultSlot > 0) viewModel.loadSlot(resultSlot)
            },
        )
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.state.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.SAVE_TO_ALBUM,
                        onClick = { openSaves() },
                    ),
                    center = LightTopBarCenter.Text("Zork III"),
                    rightButton = LightBarButton.LightIcon(
                        icon = LightIcons.SETTINGS,
                        onClick = {
                            navigateTo(
                                screenFactory = { activity -> ZorkAboutScreen(activity, viewModel::restart) },
                                resultCallback = { },
                            )
                        },
                    ),
                )

                Box(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LightText(
                            text = state.statusLine,
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            align = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.5f.gridUnitsAsDp()),
                        )
                        LightScrollView {
                            LightText(
                                text = state.transcript,
                                variant = LightTextVariant.Copy,
                                align = TextAlign.Left,
                                modifier = Modifier.padding(horizontal = 2f.gridUnitsAsDp()),
                            )
                        }
                    }
                }

                LightTextField(
                    label = "Command",
                    value = state.input,
                    placeholder = "Tap to type a command",
                    onClick = {
                        navigateTo(
                            screenFactory = { activity -> ZorkInputScreen(activity, state.input) },
                            resultCallback = { result: String? -> result?.let { viewModel.submit(it) } },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
                )
            }
        }
    }
}
