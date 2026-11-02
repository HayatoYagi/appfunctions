/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.appfunctions.agent.ui.components

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.appfunctions.agent.R
import com.example.appfunctions.agent.domain.a2ui.A2uiPayload
import com.example.appfunctions.agent.domain.a2ui.parseA2uiContent
import com.example.appfunctions.agent.ui.theme.GoogleSansCodeFontFamily
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The tallest a surface can be, which also gives lazy Basic Catalog lists bounded height. */
private val MaxSurfaceHeight = 480.dp

/**
 * Renders an A2UI resource returned by an AppFunction, as a host such as Gemini might.
 *
 * Only the A2UI Basic Catalog is supported. The renderer reports other catalogs as errors. User
 * actions are listed below the surface rather than sent anywhere.
 *
 * @param content The content of the A2UI resource.
 * @param modifier The modifier to apply to the preview.
 */
@Composable
fun A2uiResourcePreview(
    content: String,
    modifier: Modifier = Modifier,
) {
    val payload = remember(content) { parseA2uiContent(content) }
    var showRawJson by rememberSaveable(content) { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = payload.agentDisplayName ?: stringResource(R.string.a2ui_preview_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (payload.error != null) {
                Text(
                    text = payload.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                A2uiSurfaces(payload)
            }

            TextButton(onClick = { showRawJson = !showRawJson }) {
                Text(
                    stringResource(
                        if (showRawJson) R.string.a2ui_hide_json else R.string.a2ui_show_json,
                    ),
                )
            }
            if (showRawJson) {
                SelectionContainer {
                    Text(
                        text = content,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = GoogleSansCodeFontFamily,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun A2uiSurfaces(payload: A2uiPayload) {
    val context = LocalContext.current
    val catalog =
        remember(context) {
            materialA2uiBasicCatalogV1(
                image =
                    MaterialA2uiBasicCatalogV1Defaults.image { url, description, scale, modifier, onError ->
                        AsyncImage(
                            model = url,
                            contentDescription = description,
                            contentScale = scale,
                            modifier = modifier,
                            onError = { state -> onError(state.result.throwable) },
                        )
                    },
                video =
                    MaterialA2uiBasicCatalogV1Defaults.video { _, modifier, _ ->
                        Text(stringResource(R.string.a2ui_media_unsupported), modifier)
                    },
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, modifier, _ ->
                        Text(stringResource(R.string.a2ui_media_unsupported), modifier)
                    },
                functions = A2uiBasicCatalogV1.Functions(context),
            )
        }
    val processor = remember(payload, catalog) { A2uiMessageProcessor(catalogs = listOf(catalog)) }
    val surfaces by processor.activeSurfaces.collectAsStateWithLifecycle()
    val events = remember(processor) { mutableStateListOf<String>() }
    val errors = remember(processor) { mutableStateListOf<String>() }

    LaunchedEffect(processor) {
        launch(Dispatchers.Default) { processor.collectMessages() }
        launch(start = CoroutineStart.UNDISPATCHED) {
            processor.outboundEvents.collect { message ->
                when (message) {
                    is A2uiClientEventMessage -> events.add("${message.type} ${message.context}")
                    is A2uiClientErrorMessage -> errors.add("${message.code}: ${message.message}")
                }
            }
        }
        val parser = A2uiMessageParser()
        payload.messages.forEach { processor.processInput(parser, it) }
    }

    MaterialTheme(colorScheme = brandedColorScheme(payload.primaryColor)) {
        surfaces.forEach { surface ->
            key(surface.id) {
                A2uiSurface(
                    surfaceModel = surface,
                    modifier = Modifier.fillMaxWidth().heightIn(max = MaxSurfaceHeight),
                )
            }
        }
    }

    errors.forEach { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    events.forEach { event ->
        Text(
            text = stringResource(R.string.a2ui_action_sent, event),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Applies a brand color to primary elements only. The Connected Apps UI guidelines reserve brand
 * colors for elements such as the primary call to action, and leave everything else to the host.
 */
@Composable
private fun brandedColorScheme(primaryColor: String?) =
    MaterialTheme.colorScheme.let { scheme ->
        val primary =
            remember(primaryColor) {
                primaryColor?.let { runCatching { Color(it.toColorInt()) }.getOrNull() }
            }
        if (primary == null) {
            scheme
        } else {
            scheme.copy(
                primary = primary,
                onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
