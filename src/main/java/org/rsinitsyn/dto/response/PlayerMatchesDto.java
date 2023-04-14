package org.rsinitsyn.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.rsinitsyn.domain.MatchType;

@Data
@AllArgsConstructor
public class PlayerMatchesDto {
    private List<PlayerMatchDetailsDto> matches;
    private List<String> formattedMatches;

    @Builder
    @Data
    public static class PlayerMatchDetailsDto {
        private MatchType matchType;
        private String name;
        private int score;
        private int opponentScore;
        private String opponentName;
        private String stage;
        private int number;

        public String getRepresentation() {
            return String.format("%s %d - %d %s",
                    name, score, opponentScore, opponentName);
        }

        public int getScoreDifference() {
            return score - opponentScore;
        }
    }
}
