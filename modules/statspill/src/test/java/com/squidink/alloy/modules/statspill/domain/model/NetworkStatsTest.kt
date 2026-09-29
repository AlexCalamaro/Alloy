package com.squidink.alloy.modules.statspill.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStatsTest {

    @Test
    fun `NetworkStats has correct default id and name`() {
        val stats = NetworkStats()

        assertEquals("network", stats.id)
        assertEquals("Network", stats.name)
        assertEquals(StatCategory.NETWORK, stats.category)
    }

    @Test
    fun `NetworkStats implements StatType interface`() {
        val stats: StatType = NetworkStats(
            timestamp = 1234567890L,
            rxBytes = 1000L,
            txBytes = 500L,
            rxBytesPerSecond = 100f,
            txBytesPerSecond = 50f
        )

        assertNotNull(stats)
        assertEquals(StatCategory.NETWORK, stats.category)
    }

    @Test
    fun `NetworkStats stores values correctly`() {
        val stats = NetworkStats(
            timestamp = 1234567890L,
            rxBytes = 1_000_000_000L,
            txBytes = 500_000_000L,
            rxBytesPerSecond = 1024f,
            txBytesPerSecond = 512f
        )

        assertEquals(1_000_000_000L, stats.rxBytes)
        assertEquals(500_000_000L, stats.txBytes)
        assertEquals(1024f, stats.rxBytesPerSecond)
        assertEquals(512f, stats.txBytesPerSecond)
    }

    @Test
    fun `NetworkStats default values are correct`() {
        val stats = NetworkStats()

        assertEquals(0L, stats.rxBytes)
        assertEquals(0L, stats.txBytes)
        assertEquals(0f, stats.rxBytesPerSecond)
        assertEquals(0f, stats.txBytesPerSecond)
    }

    @Test
    fun `NetworkStats timestamp defaults to current time when not specified`() {
        val before = System.currentTimeMillis()
        val stats = NetworkStats()
        val after = System.currentTimeMillis()

        assertTrue(stats.timestamp >= before)
        assertTrue(stats.timestamp <= after)
    }

    @Test
    fun `formatLinkSpeed formats Gbps and Mbps correctly`() {
        val gigabit = NetworkStats(linkDownstreamBandwidthKbps = 1_200_000)
        assertEquals("1.2 Gbps", gigabit.formatLinkSpeed())

        val fast = NetworkStats(linkDownstreamBandwidthKbps = 100_000)
        assertEquals("100 Mbps", fast.formatLinkSpeed())

        val slow = NetworkStats(linkDownstreamBandwidthKbps = 512)
        assertEquals("512 Kbps", slow.formatLinkSpeed())

        val none = NetworkStats(linkDownstreamBandwidthKbps = null)
        assertEquals(null, none.formatLinkSpeed())
    }

    @Test
    fun `NetworkTransportType display names are correct`() {
        assertEquals("Wi-Fi", NetworkTransportType.WIFI.getDisplayName())
        assertEquals("Cellular", NetworkTransportType.CELLULAR.getDisplayName())
        assertEquals("Ethernet", NetworkTransportType.ETHERNET.getDisplayName())
        assertEquals("VPN", NetworkTransportType.VPN.getDisplayName())
        assertEquals("Offline", NetworkTransportType.NONE.getDisplayName())
    }
}
