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
    fun `remote finalization awards exactly one current set`() {
        assertEquals(
            1 to 0,
            MatchFinalizer.setsAfterAwardingCurrentSet(
                winner = Team.TEAM_1,
                team1Sets = 0,
                team2Sets = 0,
            ),
        )
    }

    @Test
    fun `remote finalization preserves recorded sets and awards the current set`() {
        assertEquals(
            1 to 1,
            MatchFinalizer.setsAfterAwardingCurrentSet(
                winner = Team.TEAM_2,
                team1Sets = 1,
                team2Sets = 0,
            ),
        )
    }

    @Test
    fun `tie keeps existing set totals`() {
        assertEquals(
            1 to 1,
            MatchFinalizer.setsAfterAwardingCurrentSet(
                winner = null,
                team1Sets = 1,
                team2Sets = 1,
            ),
        )
    }
}
