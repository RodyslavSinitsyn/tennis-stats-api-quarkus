package org.rsinitsyn.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.domain.MatchType;

@Data
@AllArgsConstructor
public class PlayerProgressResponse {
    private MatchType filter;
    private int intervals;
    private List<PlayerProgressIntervalDto> dayInterval;

    @AllArgsConstructor
    @Data
    public static class PlayerProgressIntervalDto {
        private LocalDate start;
        private LocalDate end;
        private PlayerProgressDifferenceListDto list;
    }

    @Builder
    @Data
    public static class PlayerProgressDifferenceListDto {
        private PlayerProgressDifferenceDto winRate;
        private PlayerProgressDifferenceDto avgPointsScored;
        private PlayerProgressDifferenceDto avgPointsMissed;
        private PlayerProgressDifferenceDto pointsRate;
    }

    @Data
    @AllArgsConstructor
    public static class PlayerProgressDifferenceDto {
        private String name;
        private double before;
        private double after;
        @JsonIgnore
        private double diff;
        @JsonIgnore
        private double diffPercent;

        public String diff() {
            return appendPlusSymbolIfNeeded(diff);
        }

        @JsonProperty
        public String progress() {
            return appendPlusSymbolIfNeeded(diffPercent) + "%" + " (" + diff() + ")";
        }

        private String appendPlusSymbolIfNeeded(double val) {
            return val > 0
                    ? "+" + val
                    : "" + val;
        }
    }
}
