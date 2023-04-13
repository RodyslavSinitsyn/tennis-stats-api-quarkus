package org.rsinitsyn.dto.request;

import javax.ws.rs.QueryParam;
import lombok.Data;
import org.rsinitsyn.domain.MatchType;

@Data
public class PlayerStatsFilters {
    @QueryParam("opponent")
    private String opponent;
    @QueryParam("tournament")
    private String tournament;
    @QueryParam("type")
    private MatchType type;
}
