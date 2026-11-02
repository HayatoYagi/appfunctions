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
package com.example.appfunctions.agent.domain.a2ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** The MIME type of an A2UI resource returned by an AppFunction. */
const val A2UI_MIME_TYPE = "application/a2ui+json"

/**
 * An A2UI resource split into the protocol messages to pass to the renderer.
 *
 * @property messages The individual A2UI messages, each as a JSON object string.
 * @property primaryColor The brand color from `createSurface.theme`, if there is one.
 * @property agentDisplayName The display name from `createSurface.theme`, if there is one.
 * @property error A description of why the resource can't be rendered, if it can't.
 */
data class A2uiPayload(
    val messages: List<String> = emptyList(),
    val primaryColor: String? = null,
    val agentDisplayName: String? = null,
    val error: String? = null,
)

/**
 * Finds the content of every A2UI resource in an AppFunction result.
 *
 * @param resultJson The AppFunction result as JSON.
 * @return The content of each resource whose MIME type is [A2UI_MIME_TYPE], in document order.
 */
fun extractA2uiResourceContents(resultJson: String): List<String> {
    val root = runCatching { Json.parseToJsonElement(resultJson) }.getOrNull() ?: return emptyList()
    return buildList { collectA2uiResources(root, this) }
}

private fun collectA2uiResources(
    element: JsonElement,
    contents: MutableList<String>,
) {
    when (element) {
        is JsonArray -> element.forEach { collectA2uiResources(it, contents) }
        is JsonObject -> {
            val mimeType = element.stringOrNull("mimeType")
            val content = element.stringOrNull("content")
            if (content != null && mimeType?.isA2uiMimeType() == true) {
                contents.add(content)
            } else {
                element.values.forEach { collectA2uiResources(it, contents) }
            }
        }
        is JsonPrimitive -> Unit
    }
}

/**
 * Splits the content of an A2UI resource into protocol messages.
 *
 * This checks only the envelope. The A2UI renderer validates the messages themselves.
 *
 * @param content A JSON array of A2UI messages, or a single A2UI message.
 * @return The parsed payload. [A2uiPayload.error] is set if the content can't be rendered.
 */
fun parseA2uiContent(content: String): A2uiPayload {
    val root =
        runCatching { Json.parseToJsonElement(content) }.getOrElse {
            return A2uiPayload(error = "Invalid JSON: ${it.message}")
        }
    val elements =
        when (root) {
            is JsonArray -> root
            is JsonObject -> listOf(root)
            is JsonPrimitive -> return A2uiPayload(error = "Expected a JSON array or object")
        }
    if (elements.isEmpty()) {
        return A2uiPayload(error = "The resource has no A2UI messages")
    }
    if (elements.any { it !is JsonObject }) {
        return A2uiPayload(error = "Every A2UI message must be a JSON object")
    }

    val theme =
        elements
            .firstNotNullOfOrNull { (it as JsonObject)["createSurface"] as? JsonObject }
            ?.get("theme") as? JsonObject
    return A2uiPayload(
        messages = elements.map { it.toString() },
        primaryColor = theme?.stringOrNull("primaryColor"),
        agentDisplayName = theme?.stringOrNull("agentDisplayName"),
    )
}

private fun JsonObject.stringOrNull(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

private fun String.isA2uiMimeType(): Boolean = substringBefore(';').trim().equals(A2UI_MIME_TYPE, ignoreCase = true)
