package com.hsiaower.scoreboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchFinalizerTest {
    @Test
    fun `higher team one score wins finalized match`() {
        assertEquals(Team.TEAM_1, MatchFinalizer.winnerFromScore(18, 12))
    }

    @Test
    fun `higher team two score wins finalized match`() {
        assertEquals(Team.TEAM_2, MatchFinalizer.winnerFromScore(7, 15))
    }

    @Test
    fun `tied score finalizes without a winner`() {
        assertNull(MatchFinalizer.winnerFromScore(10, 10))
    }
}
