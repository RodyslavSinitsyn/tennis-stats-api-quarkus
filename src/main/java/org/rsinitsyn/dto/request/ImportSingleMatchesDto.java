package org.rsinitsyn.dto.request;

import lombok.Data;
import org.rsinitsyn.domain.MatchType;

@Data
public class ImportSingleMatchesDto {
    private String content;
    private MatchType matchType;
    private CreateMatchDto.TournamentInfo tournamentInfo;
}
