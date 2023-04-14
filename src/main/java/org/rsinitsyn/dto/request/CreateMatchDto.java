package org.rsinitsyn.dto.request;

import java.util.Optional;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.domain.TournamentStage;

public record CreateMatchDto(MatchType type, PlayerResultDto player, PlayerResultDto opponentPlayer,
                             TournamentInfo tournamentInfo) {
    public record PlayerResultDto(String name, int score) {
    }

    public record TournamentInfo(String name, Optional<TournamentStage> stage) {
    }
}
