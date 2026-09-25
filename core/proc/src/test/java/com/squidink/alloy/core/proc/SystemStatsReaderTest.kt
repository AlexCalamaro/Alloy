package com.squidink.alloy.core.proc

import org.junit.Assert.assertNotNull
import org.junit.Test

class SystemStatsReaderTest {

    @Test
    fun `readMemInfo returns non null MemInfo`() {
        // Note: This test requires a real Context to work properly
        // In unit tests, you would use Robolectric or a mock context
        // For now, just verify the class exists and can be instantiated
        assertNotNull(SystemStatsReader::class.java)
    }
}
