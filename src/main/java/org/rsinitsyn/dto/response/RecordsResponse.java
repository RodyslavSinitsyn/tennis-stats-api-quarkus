package org.rsinitsyn.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecordsResponse {

    private Map<String, RecordListDto> records;

    @Builder
    @Data
    public static class RecordListDto {
        private RecordDto matches;
        private RecordDto wins;
        private RecordDto loses;
        private RecordDto winRate;
        private RecordDto pointsScored;
        private RecordDto avgPointsScored;
        private RecordDto pointsMissed;
        private RecordDto avgPointsMissed;
        private RecordDto pointsRate;

        @Data
        @AllArgsConstructor
        public static class RecordDto {
            private PlayerValueDto highest;
            private PlayerValueDto lowest;
        }

        public record PlayerValueDto(String holder, String value) {
        }
    }
}
