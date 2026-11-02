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
package com.example.chatapp.appfunctions

import org.json.JSONArray
import org.json.JSONObject

/** Builds a message preview card with the A2UI Basic Catalog. */
internal object MessageCardA2uiBuilder {
    private const val VERSION = "v0.9"
    private const val BASIC_CATALOG_ID = "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"

    /**
     * Builds the A2UI messages for a card.
     *
     * @param endpointValue The identifier of the recipient, sent back with button actions.
     * @param displayName The name of the recipient.
     * @param messageBody The message to preview.
     * @return A JSON array of A2UI messages.
     */
    fun build(
        endpointValue: String,
        displayName: String,
        messageBody: String,
    ): String {
        val surfaceId = "message_card_$endpointValue"
        val actionContext =
            JSONObject()
                .put("endpointValue", endpointValue)
                .put("messageBody", messageBody)

        val components =
            JSONArray()
                .put(component("root", "Card").put("child", "content"))
                .put(column("content", "header", "divider", "body", "actions"))
                .put(
                    component("header", "Row")
                        .put("align", "center")
                        .put("children", JSONArray().put("icon").put("titles")),
                )
                .put(component("icon", "Icon").put("name", "person"))
                .put(column("titles", "recipient", "app"))
                .put(text("recipient", displayName, "h4"))
                .put(text("app", "New message in ChatApp", "caption"))
                .put(component("divider", "Divider"))
                .put(text("body", messageBody, "body"))
                .put(
                    component("actions", "Row")
                        .put("justify", "end")
                        .put("children", JSONArray().put("edit").put("send")),
                )
                .put(text("edit_label", "Edit", "body"))
                .put(button("edit", "edit_label", "borderless", "edit_message", actionContext))
                .put(text("send_label", "Send", "body"))
                .put(button("send", "send_label", "primary", "send_message", actionContext))

        val createSurface =
            JSONObject()
                .put("version", VERSION)
                .put(
                    "createSurface",
                    JSONObject()
                        .put("surfaceId", surfaceId)
                        .put("catalogId", BASIC_CATALOG_ID)
                        .put(
                            "theme",
                            JSONObject()
                                .put("primaryColor", "#1A73E8")
                                .put("agentDisplayName", "ChatApp"),
                        ),
                )
        val updateComponents =
            JSONObject()
                .put("version", VERSION)
                .put(
                    "updateComponents",
                    JSONObject()
                        .put("surfaceId", surfaceId)
                        .put("components", components),
                )
        return JSONArray().put(createSurface).put(updateComponents).toString()
    }

    private fun component(
        id: String,
        type: String,
    ): JSONObject = JSONObject().put("id", id).put("component", type)

    private fun column(
        id: String,
        vararg children: String,
    ): JSONObject = component(id, "Column").put("children", JSONArray(children.toList()))

    private fun text(
        id: String,
        text: String,
        variant: String,
    ): JSONObject = component(id, "Text").put("text", text).put("variant", variant)

    private fun button(
        id: String,
        labelId: String,
        variant: String,
        eventName: String,
        context: JSONObject,
    ): JSONObject =
        component(id, "Button")
            .put("child", labelId)
            .put("variant", variant)
            .put(
                "action",
                JSONObject().put(
                    "event",
                    JSONObject().put("name", eventName).put("context", context),
                ),
            )
}
