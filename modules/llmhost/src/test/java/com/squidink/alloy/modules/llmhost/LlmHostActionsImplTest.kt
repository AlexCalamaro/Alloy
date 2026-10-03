package com.squidink.alloy.modules.llmhost

import com.squidink.alloy.core.feature.FeatureIds
import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LlmHostActionsImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var fakeRepository: FakeLlmHostRepository
    private lateinit var actions: LlmHostActionsImpl

    @Before
    fun setUp() {
        fakeRepository = FakeLlmHostRepository()
        actions = LlmHostActionsImpl(
            repository = fakeRepository,
            applicationScope = testScope
        )
    }

    @Test
    fun `metadata matches feature definition`() {
        assertEquals(FeatureIds.LLM_HOST, actions.moduleId)
        assertEquals("LLM Host", actions.displayName)
        assertEquals(Screens.LlmHost.route, actions.screenRoute)
    }

    @Test
    fun `isHostRunning reflects repository status without blocking`() {
        fakeRepository.hostStatusFlow.value = HostStatus.STOPPED
        assertFalse(actions.isHostRunning())

        fakeRepository.hostStatusFlow.value = HostStatus.RUNNING
        assertTrue(actions.isHostRunning())

        fakeRepository.hostStatusFlow.value = HostStatus.STARTING
        assertFalse(actions.isHostRunning())
    }

    @Test
    fun `startHost triggers repository startServer`() = runTest(testDispatcher) {
        actions.startHost()
        assertEquals(HostStatus.RUNNING, fakeRepository.hostStatusFlow.value)
        assertTrue(actions.isHostRunning())
    }

    @Test
    fun `stopHost triggers repository stopServer`() = runTest(testDispatcher) {
        fakeRepository.hostStatusFlow.value = HostStatus.RUNNING
        actions.stopHost()
        assertEquals(HostStatus.STOPPED, fakeRepository.hostStatusFlow.value)
        assertFalse(actions.isHostRunning())
    }

    @Test
    fun `query calls repository test prompt and returns response text`() = runTest(testDispatcher) {
        val result = actions.query("Explain memory")
        assertTrue(result.contains("operating system", ignoreCase = true))
    }
}
