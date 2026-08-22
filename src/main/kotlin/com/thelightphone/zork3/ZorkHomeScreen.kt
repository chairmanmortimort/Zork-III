package com.thelightphone.zork3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

@InitialScreen
class ZorkHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, ZorkViewModel>(sealedActivity) {

    override val viewModelClass: Class<ZorkViewModel>
        get() = ZorkViewModel::class.java

    override fun createViewModel() = ZorkViewModel()

    override fun onAppPause() {
        super.onAppPause()
        viewModel.saveGame(lightContext.filesDir)
    }

    override fun onScreenDestroy() {
        super.onScreenDestroy()
        viewModel.saveGame(lightContext.filesDir)
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val text by viewModel.text.collectAsState()
        val isWaiting by viewModel.isWaiting.collectAsState()
        val hasQuit by viewModel.hasQuit.collectAsState()

        LightTheme(colors = themeColors) {
            LaunchedEffect(Unit) {
                viewModel.setAssetReader { path -> lightContext.readAsset(path) }
                viewModel.startGame(
                    readAsset = { path -> lightContext.readAsset(path) },
                    filesDir = lightContext.filesDir,
                )
            }

            Column(modifier = Modifier.fillMaxSize().background(LightThemeTokens.colors.background)) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.SAVE_TO_ALBUM,
                        onClick = { openSaves() },
                    ),
                    center = LightTopBarCenter.Text("Zork III"),
                    rightButton = LightBarButton.LightIcon(
                        icon = LightIcons.SETTINGS,
                        onClick = { openAbout() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1f.gridUnitsAsDp()),
                )

                gameTextDisplay(
                    text = text,
                    hasQuit = hasQuit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                if (isWaiting && !hasQuit) {
                    commandInputEditor(
                        onSubmit = { viewModel.submitInput(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1f.gridUnitsAsDp()),
                    )
                }
            }
        }
    }

    private fun openAbout() {
        navigateTo(
            screenFactory = { sa -> ZorkAboutScreen(sa) },
        ) { /* no result needed */ }
    }

    private fun openSaves() {
        val fd = lightContext.filesDir
        val slots = viewModel.listSaves(fd)
        navigateTo(
            screenFactory = { sa ->
                ZorkSavesScreen(
                    sealedActivity = sa,
                    currentSlot = 1,
                    slotList = slots,
                    onSaveToSlot = { slot -> viewModel.saveToSlot(slot, fd) },
                    onDeleteSlot = { slot -> viewModel.deleteSlot(slot, fd) },
                )
            },
        ) { resultSlot ->
            if (resultSlot != null && resultSlot > 0) viewModel.loadSlot(resultSlot, fd)
        }
    }
}

@Composable
private fun gameTextDisplay(
    text: String,
    hasQuit: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LightScrollView(modifier = modifier) {
        LightText(
            text = text.ifEmpty { "..." },
            variant = LightTextVariant.Copy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1f.gridUnitsAsDp()),
            align = TextAlign.Start,
            monospace = true,
        )
    }
    if (hasQuit) {
        LightText(
            text = "[GAME OVER — press back to exit]",
            variant = LightTextVariant.Detail,
            align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2f.gridUnitsAsDp()),
        )
    }
}

@Composable
private fun commandInputEditor(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textState = rememberTextFieldState("")
    val keyboardOptionsFlow = rememberKeyboardOptions()
    LightTextInputEditor(
        title = "Enter Command",
        state = textState,
        keyboardOptionsFlow = keyboardOptionsFlow,
        onSubmit = { result ->
            val cmd = result.toString().trim()
            if (cmd.isNotBlank()) onSubmit(cmd)
        },
        onBack = { },
        modifier = modifier,
        initialCaps = false,
        singleLine = true,
        submitLabel = "SEND",
    )
}

class ZorkAboutScreen(sealedActivity: SealedLightActivity) :
    SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        Column(modifier = Modifier.fillMaxSize().background(LightThemeTokens.colors.background)) {
            LightTopBar(
                leftButton = LightBarButton.LightIcon(
                    icon = LightIcons.BACK,
                    onClick = { goBack(Unit) },
                ),
                center = LightTopBarCenter.Text("About"),
            )
            LightScrollView(modifier = Modifier.fillMaxWidth().padding(1f.gridUnitsAsDp())) {
                Column(verticalArrangement = Arrangement.spacedBy(1f.gridUnitsAsDp())) {
                    LightText(text = "Zork III: The Dungeon Master", variant = LightTextVariant.Title)
                    LightText(text = "Inform Interactive fiction - a fantasy story", variant = LightTextVariant.Copy)
                    LightText(text = "Copyright (c) 2025", variant = LightTextVariant.Copy)
                    LightText(text = "All rights Reserved", variant = LightTextVariant.Copy)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    LightText(text = "In 2025, Microsoft released the original Zork III source code and the Z-machine specification under the MIT License, making the game's internals fully open source for the first time.", variant = LightTextVariant.Copy)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    LightText(text = "This port runs on the Light Phone III SDK (com.thelightphone.sdk)", variant = LightTextVariant.Copy)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    LightText(text = "The interpreter is Zplet, copyright 1996, 2001 Matthew T. Risotto, released under the Artistic License.", variant = LightTextVariant.Copy)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    LightText(text = "This is an independent, non-commercial fan project. Zork and Inform are trademarks of their respective owners.", variant = LightTextVariant.Detail)
                }
            }
        }
    }
}