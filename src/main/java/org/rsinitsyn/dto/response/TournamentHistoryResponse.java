package org.rsinitsyn.dto.response;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.domain.TournamentStage;

@AllArgsConstructor
@Data
public class TournamentHistoryResponse {
    private Tournament tournamentInfo;
    private List<String> table;
    private Map<TournamentStage, List<String>> matchesHistory;

    @Data
    @AllArgsConstructor
    public static class PlayerLeagueResultDto implements Comparable<PlayerLeagueResultDto> {
        private String name;
        private int scored;
        private int missed;
        private double rate;
        private int wins;
        private int loses;

        @Override
        public int compareTo(PlayerLeagueResultDto other) {
            return Comparator.comparingInt(PlayerLeagueResultDto::getWins)
                    .thenComparingDouble(PlayerLeagueResultDto::getRate)
                    .thenComparingInt(PlayerLeagueResultDto::getScored)
                    .reversed()
                    .compare(this, other);
        }
    }
}
