package org.rsinitsyn.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.rsinitsyn.utils.StatsUtils;

@AllArgsConstructor
@Data
public class MatchPredictionResponse {

    private double sum;
    private String leftPlayer;
    private String rightPlayer;
    private List<MatchPredictDto> predictions;

    @AllArgsConstructor
    @Data
    public static class MatchPredictDto implements Comparable<MatchPredictDto> {
        @JsonIgnore
        private int scored;
        @JsonIgnore
        private int missed;
        private double probability;

        public MatchPredictDto(int scored, int missed) {
            this.scored = scored;
            this.missed = missed;
        }

        @JsonProperty
        public String score() {
            return scored + " - " + missed;
        }

        @Override
        public int compareTo(MatchPredictDto other) {
            return Comparator.comparing(MatchPredictDto::getProbability, Comparator.reverseOrder())
                    .thenComparing(dto -> StatsUtils.divide(dto.scored, dto.missed), Comparator.reverseOrder())
                    .compare(this, other);
        }
    }
}
