package org.rsinitsyn.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerHistoryResponse {
    private PlayerStatsHistoryListDto allHistory;
    private PlayerStatsHistoryListDto shortHistory;
    private PlayerStatsHistoryListDto longHistory;

    @Builder
    @Data
    public static class PlayerStatsHistoryListDto {
        private List<Double> winRate;
        private List<Double> avgPointsScored;
        private List<Double> avgPointsMissed;
        private List<Double> pointsRate;
    }
}
