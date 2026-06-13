package com.hsiaower.scoreboard.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreTimelineTest {
    @Test
    fun `events preserve point order between teams`() {
        val events = ScoreTimeline.pointEvents(
            listOf(
                ScoreSnapshot(0, 0),
                ScoreSnapshot(1, 0),
                ScoreSnapshot(1, 1),
                ScoreSnapshot(2, 1),
            ),
        )

        assertEquals(
            listOf(
                PointEvent(Team.TEAM_1, 1),
                PointEvent(Team.TEAM_2, 1),
                PointEvent(Team.TEAM_1, 2),
            ),
            events,
        )
    }

    @Test
    fun `manual score jump expands into individual points`() {
        val events = ScoreTimeline.pointEvents(
            listOf(
                ScoreSnapshot(0, 0),
                ScoreSnapshot(3, 0),
            ),
        )

        assertEquals(listOf(1, 2, 3), events.map { it.score })
    }

    @Test
    fun `score correction removes latest point for that team`() {
        val events = ScoreTimeline.pointEvents(
            listOf(
                ScoreSnapshot(0, 0),
                ScoreSnapshot(1, 0),
                ScoreSnapshot(1, 1),
                ScoreSnapshot(2, 1),
                ScoreSnapshot(1, 1),
            ),
        )

        assertEquals(
            listOf(
                PointEvent(Team.TEAM_1, 1),
                PointEvent(Team.TEAM_2, 1),
            ),
            events,
        )
    }
}
