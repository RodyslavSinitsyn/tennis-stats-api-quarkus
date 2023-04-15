package org.rsinitsyn.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.dto.request.PlayerStatsFilters;

@Data
@AllArgsConstructor
public class PlayerStatsDto {
    private String playerShortName;
    private PlayerStatsFilters filters;
    private PlayerScoreStatsDto overallStats;
    private Map<String, PlayerScoreStatsDto> typeStats;
    private Map<String, Map<String, PlayerScoreStatsDto>> versusPlayersStats;

    @Builder
    @Data
    public static final class PlayerScoreStatsDto {
        private final int matches;
        private final int wins;
        private final int loses;
        private final double winRate;
        private final int overtimes;

        private final int pointsScored;
        private final int pointsMissed;
        private final double avgPointsScored;
        private final double avgPointsMissed;
        private final int medianPointsScored;
        private final int medianPointsMissed;
        private final double pointsRate;
    }
}
