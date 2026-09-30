package com.squidink.alloy.core.feature

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeatureRegistryTest {

    private lateinit var registry: FeatureRegistry

    @Before
    fun setUp() {
        registry = FeatureRegistry.getInstance()
        registry.clear()
    }

    @After
    fun tearDown() {
        registry.clear()
    }

    @Test
    fun `registerFeature adds feature definition and enables it by default`() {
        val feature = FeatureDefinition(
            id = "test_feat",
            name = "Test Feature",
            description = "Test description",
            screenRoute = "test_route",
            category = "TestCategory",
            sortOrder = 1,
            icon = Icons.Default.Settings
        )

        registry.registerFeature(feature)

        assertEquals(1, registry.getFeatures().size)
        assertEquals(feature, registry.getFeature("test_feat"))
        assertTrue(registry.isFeatureEnabled("test_feat"))
    }

    @Test
    fun `setFeatureEnabled toggles feature state`() = runTest {
        val feature = FeatureDefinition(
            id = "toggle_feat",
            name = "Toggle Feature",
            description = "Test description",
            screenRoute = "toggle_route",
            category = "TestCategory",
            sortOrder = 2,
            icon = Icons.Default.Settings
        )

        registry.registerFeature(feature)
        assertTrue(registry.isFeatureEnabled("toggle_feat"))

        registry.setFeatureEnabled("toggle_feat", false)
        assertFalse(registry.isFeatureEnabled("toggle_feat"))

        registry.setFeatureEnabled("toggle_feat", true)
        assertTrue(registry.isFeatureEnabled("toggle_feat"))
    }

    @Test
    fun `unregisterFeature removes feature from registry`() {
        val feature = FeatureDefinition(
            id = "remove_feat",
            name = "Remove Feature",
            description = "Test description",
            screenRoute = "remove_route",
            category = "TestCategory",
            sortOrder = 3,
            icon = Icons.Default.Settings
        )

        registry.registerFeature(feature)
        assertEquals(1, registry.getFeatures().size)

        registry.unregisterFeature("remove_feat")
        assertEquals(0, registry.getFeatures().size)
        assertNull(registry.getFeature("remove_feat"))
    }
}
