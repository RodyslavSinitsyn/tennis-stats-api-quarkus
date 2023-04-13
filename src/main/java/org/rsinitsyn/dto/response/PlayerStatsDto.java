package org.rsinitsyn.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.dto.request.PlayerStatsFilters;

@Data
@AllArgsConstructor
public class PlayerStatsDto {
    private PlayerStatsFilters filters;
    private PlayerScoreStatsDto overallStats;
    private Map<MatchType, PlayerScoreStatsDto> typeStats;
    private Map<String, List<PlayerScoreStatsDto>> versusPlayersStats;

    @Builder
    @Data
    public static final class PlayerScoreStatsDto {
        private final MatchType matchType;
        private final int matches;
        private final int wins;
        private final int loses;
        private final double winRate;
        private final int scored;
        private final int missed;
        private final double scoreRate;
    }
}
