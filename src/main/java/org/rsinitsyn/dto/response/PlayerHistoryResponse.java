package org.rsinitsyn.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerHistoryResponse {
    private int matchesChunkSize;
    private Map<String, PlayerStatsHistoryListDto> history;

    @Builder
    @Data
    public static class PlayerStatsHistoryListDto {
        private int matchesCount;
        private List<Double> winRate;
        private List<Double> pointsScored;
        private List<Double> avgPointsScored;
        private List<Double> pointsMissed;
        private List<Double> avgPointsMissed;
        private List<Double> pointsRate;
    }
}
