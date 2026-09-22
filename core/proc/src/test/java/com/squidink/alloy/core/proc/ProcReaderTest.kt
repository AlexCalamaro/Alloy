package com.squidink.alloy.core.proc

import org.junit.Assert.assertNotNull
import org.junit.Test

class ProcReaderTest {

    @Test
    fun `readMemInfo returns non null MemInfo`() {
        val procReader = ProcReader()
        val memInfo = procReader.readMemInfo()
        assertNotNull(memInfo)
    }
}
