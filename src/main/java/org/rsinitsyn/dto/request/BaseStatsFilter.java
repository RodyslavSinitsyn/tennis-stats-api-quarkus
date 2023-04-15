package org.rsinitsyn.dto.request;

import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rsinitsyn.domain.TournamentStage;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseStatsFilter {
    @QueryParam("tournament")
    private String tournament;
    @QueryParam("stage")
    private TournamentStage stage;
}