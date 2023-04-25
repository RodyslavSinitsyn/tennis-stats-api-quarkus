package org.rsinitsyn.dto.response;

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
        private double before;
        private double after;
        private String diff;
        private String diffPercent;
    }
}
