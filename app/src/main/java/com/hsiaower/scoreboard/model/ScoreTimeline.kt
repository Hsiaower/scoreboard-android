package com.hsiaower.scoreboard.model

data class PointEvent(
    val team: Team,
    val score: Int,
)

object ScoreTimeline {
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
                events += PointEvent(Team.TEAM_1, team1Score)
            }
            while (team2Score < snapshot.team2Score) {
                team2Score++
                events += PointEvent(Team.TEAM_2, team2Score)
            }
        }

        return events
    }
}
