package org.rsinitsyn.dto.request;

import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rsinitsyn.domain.TournamentStage;

@Data
@NoArgsConstructor
public class PlayerFilters extends BaseFilter {
    @QueryParam("opponent")
    private String opponent;

    public PlayerFilters(String tournament, TournamentStage stage, String opponent) {
        super(tournament, stage);
        this.opponent = opponent;
    }
}
