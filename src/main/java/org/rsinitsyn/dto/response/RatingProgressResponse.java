package org.rsinitsyn.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.domain.MatchType;

@AllArgsConstructor
@Data
public class RatingProgressResponse {
    private MatchType matchType;
    private int matchesInterval;
    private RatingProgressListDto progressRating;

    @Data
    @Builder
    public static class RatingProgressListDto {
        private List<PlayerProgressResponse.PlayerProgressDifferenceDto> winRate;
        private List<PlayerProgressResponse.PlayerProgressDifferenceDto> avgScored;
        private List<PlayerProgressResponse.PlayerProgressDifferenceDto> avgMissed;
        private List<PlayerProgressResponse.PlayerProgressDifferenceDto> pointsRate;
    }
}
