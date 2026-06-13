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

    @Test
    fun `winner receives configured match set target`() {
        assertEquals(
            2 to 1,
            MatchFinalizer.finalizedSets(
                winner = Team.TEAM_1,
                team1Sets = 0,
                team2Sets = 1,
                setsToWin = 2,
            ),
        )
    }

    @Test
    fun `finalization does not lower an existing set total`() {
        assertEquals(
            3 to 1,
            MatchFinalizer.finalizedSets(
                winner = Team.TEAM_1,
                team1Sets = 3,
                team2Sets = 1,
                setsToWin = 2,
            ),
        )
    }

    @Test
    fun `tie keeps existing set totals`() {
        assertEquals(
            1 to 1,
            MatchFinalizer.finalizedSets(
                winner = null,
                team1Sets = 1,
                team2Sets = 1,
                setsToWin = 2,
            ),
        )
    }
}
