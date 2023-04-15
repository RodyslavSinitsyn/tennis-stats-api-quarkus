package org.rsinitsyn.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.domain.TournamentStage;

@AllArgsConstructor
@Data
public class TournamentHistoryDto {
    private Tournament tournamentInfo;
    private Map<Integer, Player> positions;
    private Map<TournamentStage, List<String>> matchesHistory;
}
