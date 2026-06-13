package com.hsiaower.scoreboard.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchTimelineTest {
    @Test
    fun `new timeline has no activity`() {
        assertFalse(MatchTimeline().hasActivity)
    }

    @Test
    fun `score event marks timeline active`() {
        val timeline = MatchTimeline(
            currentSetEvents = listOf(
                ScoreSnapshot(0, 0),
                ScoreSnapshot(1, 0),
            ),
        )

        assertTrue(timeline.hasActivity)
    }

    @Test
    fun `completed set marks timeline active`() {
        val timeline = MatchTimeline(
            completedSets = listOf(
                SetTimeline(
                    number = 1,
                    team1Score = 25,
                    team2Score = 20,
                    winner = Team.TEAM_1,
                    events = emptyList(),
                ),
            ),
        )

        assertTrue(timeline.hasActivity)
    }
}
