package org.rsinitsyn.service;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.rsinitsyn.domain.Match;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.request.ImportSingleMatchesDto;
import org.rsinitsyn.exception.TennisApiException;

@ApplicationScoped
public class ImportService {

    @Inject
    TennisService tennisService;

    @Transactional
    public int importSingleMatches(ImportSingleMatchesDto dto) {
        List<Match> saved = new ArrayList<>();

        dto.getContent().lines().forEach(line -> {
            var tokens = line.trim().replaceAll("\\s+$", "").split(" ");
            if (tokens.length != 4) {
                throw new TennisApiException("Imported content is invalid. Token lenght: " + tokens.length, 400);
            }
            String playerName = tokens[0];
            int playerScore = Integer.parseInt(tokens[1]);
            int opponentScore = Integer.parseInt(tokens[2]);
            String opponentName = tokens[3];

            var createMatchDto = new CreateMatchDto(dto.getMatchType(),
                    new CreateMatchDto.PlayerResultDto(playerName, playerScore),
                    new CreateMatchDto.PlayerResultDto(opponentName, opponentScore),
                    dto.getTournamentInfo());
            saved.add(tennisService.saveMatch(createMatchDto));
        });

        return saved.size();
    }
}
