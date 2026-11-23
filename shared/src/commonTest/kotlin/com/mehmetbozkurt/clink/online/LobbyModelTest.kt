package com.mehmetbozkurt.clink.online

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LobbyModelTest {
    private fun member(i: Int, joinedAt: Long = i.toLong()) = Member("u$i", "P$i", "cat", joinedAt)

    @Test
    fun hostOnlineIsOnline() = assertEquals(HostStatus.Online, hostStatus(hostOnline = true, hostSeenAt = 0, now = 999_999))

    @Test
    fun hostAwayCountsDown() = assertEquals(HostStatus.Reconnecting(30), hostStatus(hostOnline = false, hostSeenAt = 1_000, now = 31_000))

    @Test
    fun hostAwayOver60sIsGone() = assertEquals(HostStatus.Gone, hostStatus(hostOnline = false, hostSeenAt = 0, now = 60_000))

    // A host that went offline before its "seen at" reached the server is treated as just gone.
    @Test
    fun hostAwayWithoutTimeCountsFromNow() = assertEquals(HostStatus.Reconnecting(60), hostStatus(hostOnline = false, hostSeenAt = null, now = 5_000))

    @Test
    fun playerOrderIsByJoinTimeAndAtMostTwelve() {
        val members = (1..14).map { member(it, joinedAt = (100 - it).toLong()) }
        val order = playerOrder(members)
        assertEquals(12, order.size)
        assertEquals("u14", order.first().uid)
    }

    @Test
    fun thirteenthIsFull() {
        val members = (1..13).map { member(it) }
        assertTrue(isFull(members, "u13"))
        assertFalse(isFull(members, "u3"))
    }
}
