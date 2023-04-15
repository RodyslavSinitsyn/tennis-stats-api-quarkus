package org.rsinitsyn.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class RatingsResponse {
    private Map<String, RatingsListDto> types;

    @Data
    @Builder
    public static class RatingsListDto {
        private List<RecordsResponse.RecordListDto.PlayerValueDto> matches;
        private List<RecordsResponse.RecordListDto.PlayerValueDto> winRate;
        private List<RecordsResponse.RecordListDto.PlayerValueDto> avgScored;
        private List<RecordsResponse.RecordListDto.PlayerValueDto> avgMissed;
        private List<RecordsResponse.RecordListDto.PlayerValueDto> pointsRate;
    }
}
