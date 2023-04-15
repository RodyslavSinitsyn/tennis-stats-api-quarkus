package org.rsinitsyn.dto.request;

import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rsinitsyn.domain.TournamentStage;

@Data
@NoArgsConstructor
public class PlayerStatsFilters extends BaseStatsFilter {
    @QueryParam("opponent")
    private String opponent;

    public PlayerStatsFilters(String tournament, TournamentStage stage, String opponent) {
        super(tournament, stage);
        this.opponent = opponent;
    }
}
