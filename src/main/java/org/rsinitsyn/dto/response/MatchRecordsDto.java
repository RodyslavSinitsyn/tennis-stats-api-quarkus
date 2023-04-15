package org.rsinitsyn.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchRecordsDto {

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
            private RecordValueDto highest;
            private RecordValueDto lowest;
        }

        public record RecordValueDto(String holder, String value) {
        }
    }
}
