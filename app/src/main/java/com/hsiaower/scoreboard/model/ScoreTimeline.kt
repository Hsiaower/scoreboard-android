package com.hsiaower.scoreboard.model

data class PointEvent(
    val team: Team,
    val score: Int,
    val team1Score: Int,
    val team2Score: Int,
    val timestamp: Long,
)

object ScoreTimeline {
    fun throughWinningPoint(
        snapshots: List<ScoreSnapshot>,
        winner: Team,
        recordedWinningScore: Int,
    ): List<ScoreSnapshot> {
        val winningIndex = snapshots.indexOfFirst { snapshot ->
            when (winner) {
                Team.TEAM_1 -> snapshot.team1Score >= recordedWinningScore
                Team.TEAM_2 -> snapshot.team2Score >= recordedWinningScore
            }
        }
        return if (winningIndex >= 0) snapshots.take(winningIndex + 1) else snapshots
    }

    fun pointEvents(snapshots: List<ScoreSnapshot>): List<PointEvent> {
        val events = mutableListOf<PointEvent>()
        var team1Score = 0
        var team2Score = 0

        snapshots.forEach { snapshot ->
            while (team1Score > snapshot.team1Score) {
                val index = events.indexOfLast { it.team == Team.TEAM_1 }
                if (index >= 0) events.removeAt(index)
                team1Score--
            }
            while (team2Score > snapshot.team2Score) {
                val index = events.indexOfLast { it.team == Team.TEAM_2 }
                if (index >= 0) events.removeAt(index)
                team2Score--
            }
            while (team1Score < snapshot.team1Score) {
                team1Score++
                events += PointEvent(
                    team = Team.TEAM_1,
                    score = team1Score,
                    team1Score = team1Score,
                    team2Score = team2Score,
                    timestamp = snapshot.timestamp,
                )
            }
            while (team2Score < snapshot.team2Score) {
                team2Score++
                events += PointEvent(
                    team = Team.TEAM_2,
                    score = team2Score,
                    team1Score = team1Score,
                    team2Score = team2Score,
                    timestamp = snapshot.timestamp,
                )
            }
        }

        return events
    }
}
