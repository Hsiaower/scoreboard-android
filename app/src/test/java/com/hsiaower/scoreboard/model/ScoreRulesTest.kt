package com.hsiaower.scoreboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreRulesTest {
    @Test
    fun `winner cannot increase after win by two victory`() {
        val scores = ScoreRules.adjust(
            team = Team.TEAM_1,
            delta = 1,
            team1Score = 26,
            team2Score = 24,
            settings = GameSettings(winningScore = 25, winByTwo = true),
        )

        assertEquals(Scores(26, 24), scores)
    }

    @Test
    fun `winner cannot increase after ordinary target victory`() {
        val scores = ScoreRules.adjust(
            team = Team.TEAM_2,
            delta = 1,
            team1Score = 20,
            team2Score = 25,
            settings = GameSettings(winningScore = 25, winByTwo = false),
        )

        assertEquals(Scores(20, 25), scores)
    }

    @Test
    fun `winner can decrease after victory`() {
        val scores = ScoreRules.adjust(
            team = Team.TEAM_1,
            delta = -1,
            team1Score = 26,
            team2Score = 24,
            settings = GameSettings(winningScore = 25, winByTwo = true),
        )

        assertEquals(Scores(25, 24), scores)
    }

    @Test
    fun `non winner remains editable after victory`() {
        val settings = GameSettings(winningScore = 25, winByTwo = true)

        assertEquals(
            Scores(26, 25),
            ScoreRules.adjust(Team.TEAM_2, 1, 26, 24, settings),
        )
        assertEquals(
            Scores(26, 23),
            ScoreRules.adjust(Team.TEAM_2, -1, 26, 24, settings),
        )
    }

    @Test
    fun `scores cannot decrease below zero`() {
        assertEquals(
            Scores(0, 0),
            ScoreRules.adjust(Team.TEAM_1, -1, 0, 0, GameSettings()),
        )
    }

    @Test
    fun `hard cap prevents score above cap`() {
        val settings = GameSettings(
            winningScore = 25,
            winByTwo = true,
            hardCapEnabled = true,
            hardCapScore = 30,
        )

        assertEquals(
            Scores(30, 29),
            ScoreRules.adjust(Team.TEAM_1, 1, 30, 29, settings),
        )
    }

    @Test
    fun `manual score validates hard cap and winner increase`() {
        val cappedSettings = GameSettings(hardCapEnabled = true, hardCapScore = 30)
        assertEquals(
            ScoreValidationError.ABOVE_HARD_CAP,
            ScoreRules.validateManualScore(Team.TEAM_1, 31, 10, 8, cappedSettings),
        )

        val winByTwoSettings = GameSettings(winningScore = 25, winByTwo = true)
        assertEquals(
            ScoreValidationError.WINNER_CANNOT_INCREASE,
            ScoreRules.validateManualScore(Team.TEAM_1, 27, 26, 24, winByTwoSettings),
        )
        assertNull(
            ScoreRules.validateManualScore(Team.TEAM_1, 25, 26, 24, winByTwoSettings),
        )
        assertNull(
            ScoreRules.validateManualScore(Team.TEAM_2, 25, 26, 24, winByTwoSettings),
        )
    }
}
