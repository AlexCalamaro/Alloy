package com.squidink.alloy.modules.statspill

import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = FakeStatsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `observeSystemStats returns stats`() = runTest {
        val stats = repository.observeSystemStats().first()
        assertNotNull(stats)
    }

    @Test
    fun `pollSystemStats returns stats`() = runTest {
        val stats = repository.pollSystemStats()
        assertNotNull(stats)
    }
    
    @Test
    fun `getMemoryPercent returns value`() = runTest {
        val percent = repository.getMemoryPercent()
        assertNotNull(percent)
    }
    
    @Test
    fun `getCpuPercent returns value`() = runTest {
        val percent = repository.getCpuPercent()
        assertNotNull(percent)
    }
}
