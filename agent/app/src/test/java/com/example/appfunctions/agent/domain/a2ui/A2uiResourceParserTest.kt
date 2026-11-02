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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class A2uiResourceParserTest {
    @Test
    fun extract_findsA2uiResources_andIgnoresOtherMimeTypes() {
        val result =
            """
            {"resources": [
              {"mimeType": "text/plain", "content": "ignored"},
              {"mimeType": "application/a2ui+json", "content": "[1]"},
              {"mimeType": "APPLICATION/A2UI+JSON; charset=utf-8", "content": "[2]"}
            ]}
            """.trimIndent()

        assertEquals(listOf("[1]", "[2]"), extractA2uiResourceContents(result))
    }

    @Test
    fun extract_findsNestedResources() {
        val result =
            """[{"item": {"resources": [{"mimeType": "application/a2ui+json", "content": "x"}]}}]"""

        assertEquals(listOf("x"), extractA2uiResourceContents(result))
    }

    @Test
    fun extract_returnsEmpty_forResultsWithoutResources() {
        assertTrue(extractA2uiResourceContents("""{"name": "Alice"}""").isEmpty())
        assertTrue(extractA2uiResourceContents("not json").isEmpty())
        assertTrue(extractA2uiResourceContents("null").isEmpty())
    }

    @Test
    fun parse_splitsMessages_andReadsTheme() {
        val content =
            """
            [
              {"version": "v0.9", "createSurface": {
                "surfaceId": "s", "catalogId": "c",
                "theme": {"primaryColor": "#1A73E8", "agentDisplayName": "ChatApp"}
              }},
              {"version": "v0.9", "updateComponents": {"surfaceId": "s", "components": []}}
            ]
            """.trimIndent()

        val payload = parseA2uiContent(content)

        assertNull(payload.error)
        assertEquals(2, payload.messages.size)
        assertTrue(payload.messages[0].contains("createSurface"))
        assertTrue(payload.messages[1].contains("updateComponents"))
        assertEquals("#1A73E8", payload.primaryColor)
        assertEquals("ChatApp", payload.agentDisplayName)
    }

    @Test
    fun parse_acceptsSingleMessage() {
        val payload = parseA2uiContent("""{"version": "v0.9", "deleteSurface": {"surfaceId": "s"}}""")

        assertNull(payload.error)
        assertEquals(1, payload.messages.size)
        assertNull(payload.primaryColor)
    }

    @Test
    fun parse_reportsInvalidContent() {
        assertNotNull(parseA2uiContent("{").error)
        assertNotNull(parseA2uiContent("42").error)
        assertNotNull(parseA2uiContent("[]").error)
        assertNotNull(parseA2uiContent("[1]").error)
    }
}
