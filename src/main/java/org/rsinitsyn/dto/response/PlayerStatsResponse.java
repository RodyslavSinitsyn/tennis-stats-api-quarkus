package org.rsinitsyn.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.dto.request.PlayerStatsFilters;

@Data
@AllArgsConstructor
public class PlayerStatsResponse {
    private String playerShortName;
    private PlayerStatsFilters filters;
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
    }
}
