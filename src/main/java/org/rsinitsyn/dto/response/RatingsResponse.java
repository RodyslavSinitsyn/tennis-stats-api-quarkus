package org.rsinitsyn.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.dto.response.RecordsResponse.PlayerValueDto;

@Data
@AllArgsConstructor
public class RatingsResponse {
    private Map<String, RatingsListDto> types;

    @Data
    @Builder
    public static class RatingsListDto {
        private List<PlayerValueDto> matches;
        private List<PlayerValueDto> winRate;
        private List<PlayerValueDto> winStreak;
        private List<PlayerValueDto> loseStreak;
        private List<PlayerValueDto> avgScored;
        private List<PlayerValueDto> avgMissed;
        private List<PlayerValueDto> pointsRate;
    }
}
