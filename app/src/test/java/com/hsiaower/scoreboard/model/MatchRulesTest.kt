package com.hsiaower.scoreboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchRulesTest {
    @Test
    fun `team reaching set target wins match`() {
        assertEquals(Team.TEAM_1, MatchRules.determineWinner(2, 1, 2))
        assertEquals(Team.TEAM_2, MatchRules.determineWinner(1, 2, 2))
    }

    @Test
    fun `sets below target do not produce match winner`() {
        assertNull(MatchRules.determineWinner(1, 1, 2))
    }

    @Test
    fun `tied sets do not produce match winner`() {
        assertNull(MatchRules.determineWinner(2, 2, 2))
    }
}
