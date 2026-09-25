package com.squidink.alloy.core.layout

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for LayoutStateRepository persistence logic.
 * Note: Full DataStore integration tests require Android instrumentation tests.
 * These are unit tests for the data model and state logic.
 */
class LayoutStateRepositoryTest {
    
    @Test
    fun `AppLayoutState DEFAULT has correct initial values`() {
        val default = AppLayoutState.DEFAULT
        assertEquals(false, default.navigationPaneOpen)
        assertEquals(false, default.detailPaneOpen)
        assertEquals("", default.currentFeature)
        assertEquals(null, default.detailFeature)
    }
    
    @Test
    fun `AppLayoutState copy creates new instance with updated values`() {
        val original = AppLayoutState.DEFAULT
        val modified = original.copy(navigationPaneOpen = true)
        
        assertEquals(false, original.navigationPaneOpen)
        assertEquals(true, modified.navigationPaneOpen)
        assertTrue(original !== modified)
    }
    
    @Test
    fun `AppLayoutState with all fields set preserves values`() {
        val state = AppLayoutState(
            navigationPaneOpen = true,
            detailPaneOpen = true,
            currentFeature = "test_feature",
            detailFeature = "test_detail"
        )
        
        assertEquals(true, state.navigationPaneOpen)
        assertEquals(true, state.detailPaneOpen)
        assertEquals("test_feature", state.currentFeature)
        assertEquals("test_detail", state.detailFeature)
    }
    
    @Test
    fun `LayoutEvent OpenDetail creates correct event`() {
        val event = LayoutEvent.OpenDetail("test_feature")
        assertEquals("test_feature", (event as LayoutEvent.OpenDetail).feature)
    }
    
    @Test
    fun `LayoutEvent SelectFeature creates correct event`() {
        val event = LayoutEvent.SelectFeature("test_feature")
        assertEquals("test_feature", (event as LayoutEvent.SelectFeature).feature)
    }
}
