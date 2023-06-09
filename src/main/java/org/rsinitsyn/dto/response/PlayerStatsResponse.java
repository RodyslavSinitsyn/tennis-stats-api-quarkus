package org.rsinitsyn.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.dto.request.OpponentFilter;

@Data
@AllArgsConstructor
public class PlayerStatsResponse {
    private String playerShortName;
    private OpponentFilter filters;
    private PlayerStatsDto overallStats;
    private Map<String, PlayerStatsDto> typeStats;
    private Map<String, Map<String, PlayerStatsDto>> versusPlayersStats;

    @Builder
    @Data
    public static final class PlayerStatsDto {
        private final int matches;
        private final int wins;
        private final int loses;
        private final double winRate;
        private final int overtimes;

        private final int winStreak;
        private final int loseStreak;

        private final int pointsScored;
        private final int pointsMissed;
        private final double avgPointsScored;
        private final double avgPointsMissed;
        private final int medianPointsScored;
        private final int medianPointsMissed;
        private final double pointsRate;

        private Map<Integer, Integer> scoredTrend;
        private Map<Integer, Integer> missedTrend;
    }
}
