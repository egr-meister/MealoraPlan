package com.mealora.plan

import com.mealora.plan.data.SerializationConfig
import com.mealora.plan.model.AppSettings
import com.mealora.plan.model.Dish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationTest {

    @Test
    fun emptyOrBlankReturnsEmptyList() {
        assertTrue(SerializationConfig.decodeList<Dish>(null).isEmpty())
        assertTrue(SerializationConfig.decodeList<Dish>("").isEmpty())
        assertTrue(SerializationConfig.decodeList<Dish>("   ").isEmpty())
    }

    @Test
    fun corruptedJsonFallsBackToEmpty() {
        assertTrue(SerializationConfig.decodeList<Dish>("{not valid json").isEmpty())
        assertTrue(SerializationConfig.decodeList<Dish>("[ this is broken ]").isEmpty())
    }

    @Test
    fun itemLevelRecoverySkipsMalformedElements() {
        // First element valid, second malformed (missing required structure).
        val raw = """[{"id":"d1","name":"Rice"}, 12345 , {"id":"d2","name":"Soup"}]"""
        val result = SerializationConfig.decodeList<Dish>(raw)
        // The two valid dish objects should survive; the stray number is skipped.
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Rice" })
        assertTrue(result.any { it.name == "Soup" })
    }

    @Test
    fun missingFieldsUseDefaults() {
        val raw = """[{"id":"d1","name":"Rice"}]"""
        val result = SerializationConfig.decodeList<Dish>(raw)
        assertEquals(1, result.size)
        val dish = result.first()
        assertEquals("Rice", dish.name)
        assertTrue(dish.ingredients.isEmpty())
        assertEquals(false, dish.favorite)
    }

    @Test
    fun roundTripDishList() {
        val dishes = listOf(Dish(id = "d1", name = "Rice"), Dish(id = "d2", name = "Soup", favorite = true))
        val encoded = SerializationConfig.encodeList(dishes)
        val decoded = SerializationConfig.decodeList<Dish>(encoded)
        assertEquals(dishes, decoded)
    }

    @Test
    fun appSettingsRoundTrip() {
        val settings = AppSettings(onboardingCompleted = true)
        val encoded = SerializationConfig.json.encodeToString(AppSettings.serializer(), settings)
        val decoded = SerializationConfig.json.decodeFromString(AppSettings.serializer(), encoded)
        assertEquals(settings, decoded)
    }
}
