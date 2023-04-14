package org.rsinitsyn.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.dto.request.PlayerStatsFilters;

@Data
@AllArgsConstructor
public class PlayerStatsDto {
    private String playerShortName;
    private PlayerStatsFilters filters;
    private PlayerScoreStatsDto overallStats;
    private Map<MatchType, PlayerScoreStatsDto> typeStats;
    private Map<String, Map<String, PlayerScoreStatsDto>> versusPlayersStats;

    @Builder
    @Data
    public static final class PlayerScoreStatsDto {
        private final MatchType matchType;

        private final int matches;
        private final int extra;
        private final int wins;
        private final int loses;
        private final double winRate;

        private final int pointsScored;
        private final double avgPointsScored;
        private final int pointsMissed;
        private final double avgPointsMissed;
        private final double pointsRate;
    }
}
