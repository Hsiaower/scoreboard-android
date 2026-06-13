package com.hsiaower.scoreboard.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreTimelineTest {
    @Test
    fun `completed set stops when winner first reaches winning score`() {
        val snapshots = listOf(
            ScoreSnapshot(17, 24),
            ScoreSnapshot(17, 25),
            ScoreSnapshot(18, 25),
            ScoreSnapshot(19, 25),
        )

        assertEquals(
            listOf(
                ScoreSnapshot(17, 24, snapshots[0].timestamp),
                ScoreSnapshot(17, 25, snapshots[1].timestamp),
            ),
            ScoreTimeline.throughWinningPoint(snapshots, Team.TEAM_2, 25),
        )
    }

    @Test
    fun `events preserve point order between teams`() {
        val firstPoint = ScoreSnapshot(1, 0)
        val secondPoint = ScoreSnapshot(1, 1)
        val thirdPoint = ScoreSnapshot(2, 1)
        val events = ScoreTimeline.pointEvents(
            listOf(
                ScoreSnapshot(0, 0),
                firstPoint,
                secondPoint,
                thirdPoint,
            ),
        )

        assertEquals(
            listOf(
                PointEvent(Team.TEAM_1, 1, 1, 0, firstPoint.timestamp),
                PointEvent(Team.TEAM_2, 1, 1, 1, secondPoint.timestamp),
                PointEvent(Team.TEAM_1, 2, 2, 1, thirdPoint.timestamp),
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
        val firstPoint = ScoreSnapshot(1, 0)
        val secondPoint = ScoreSnapshot(1, 1)
        val events = ScoreTimeline.pointEvents(
            listOf(
                ScoreSnapshot(0, 0),
                firstPoint,
                secondPoint,
                ScoreSnapshot(2, 1),
                ScoreSnapshot(1, 1),
            ),
        )

        assertEquals(
            listOf(
                PointEvent(Team.TEAM_1, 1, 1, 0, firstPoint.timestamp),
                PointEvent(Team.TEAM_2, 1, 1, 1, secondPoint.timestamp),
            ),
            events,
        )
    }

    @Test
    fun `reset preserves earlier points and restarts score counting`() {
        val beforeReset = ScoreSnapshot(2, 1)
        val reset = ScoreSnapshot(0, 0, isReset = true)
        val afterReset = ScoreSnapshot(1, 0)

        val events = ScoreTimeline.pointEvents(
            listOf(
                ScoreSnapshot(0, 0),
                beforeReset,
                reset,
                afterReset,
            ),
        )

        assertEquals(listOf(1, 2, 1, 1), events.map { it.score })
        assertEquals(
            listOf(Team.TEAM_1, Team.TEAM_1, Team.TEAM_2, Team.TEAM_1),
            events.map { it.team },
        )
    }
}
