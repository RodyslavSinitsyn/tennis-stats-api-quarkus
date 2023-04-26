package org.rsinitsyn.dto.request;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rsinitsyn.domain.TournamentStage;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseFilter {
    @QueryParam("tournament")
    private String tournament;
    @QueryParam("stage")
    private List<TournamentStage> stages = new ArrayList<>();

    public String getOpponent() {
        return null;
    }
}
