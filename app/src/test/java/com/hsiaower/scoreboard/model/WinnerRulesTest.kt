package com.hsiaower.scoreboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WinnerRulesTest {
    @Test
    fun `without win by two reaching target wins`() {
        val settings = GameSettings(winningScore = 25, winByTwo = false)

        assertEquals(Team.TEAM_1, WinnerRules.determineWinner(25, 24, settings))
    }

    @Test
    fun `win by two requires two point lead`() {
        val settings = GameSettings(winningScore = 25, winByTwo = true)

        assertNull(WinnerRules.determineWinner(25, 24, settings))
        assertEquals(Team.TEAM_1, WinnerRules.determineWinner(26, 24, settings))
    }

    @Test
    fun `hard cap wins without two point lead`() {
        val settings = GameSettings(
            winningScore = 25,
            winByTwo = true,
            hardCapEnabled = true,
            hardCapScore = 30,
        )

        assertEquals(Team.TEAM_2, WinnerRules.determineWinner(29, 30, settings))
    }

    @Test
    fun `hard cap is ignored when win by two is disabled`() {
        val settings = GameSettings(
            winningScore = 40,
            winByTwo = false,
            hardCapEnabled = true,
            hardCapScore = 30,
        )

        assertNull(WinnerRules.determineWinner(29, 30, settings))
    }

    @Test
    fun `scores below target have no winner`() {
        assertNull(WinnerRules.determineWinner(24, 23, GameSettings()))
    }
}
