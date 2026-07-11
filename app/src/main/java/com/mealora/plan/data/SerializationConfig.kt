package com.mealora.plan.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Shared JSON configuration. [ignoreUnknownKeys] and default encoding provide
 * backward- and forward-compatible deserialization so newly added fields never
 * crash older stored data and vice-versa.
 */
object SerializationConfig {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Decode a JSON array string into a list, recovering at the item level where
     * practical: if a whole-list decode fails, each element is parsed
     * independently and malformed elements are skipped rather than discarding the
     * entire list.
     */
    inline fun <reified T> decodeList(raw: String?): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        // Fast path: decode the whole list.
        try {
            return json.decodeFromString(raw)
        } catch (_: Exception) {
            // Fall through to item-level recovery.
        }
        return try {
            val array = json.parseToJsonElement(raw)
            if (array !is JsonArray) return emptyList()
            array.mapNotNull { element ->
                try {
                    json.decodeFromJsonElement<T>(element)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    inline fun <reified T> encodeList(list: List<T>): String = json.encodeToString(list)
}
