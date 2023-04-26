package org.rsinitsyn.dto.request;

import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rsinitsyn.domain.TournamentStage;

@Data
@NoArgsConstructor
public class OpponentFilter extends BaseFilter {
    @QueryParam("opponent")
    private String opponent;

    public OpponentFilter(String tournament, List<TournamentStage> stages, String opponent) {
        super(tournament, stages);
        this.opponent = opponent;
    }
}
