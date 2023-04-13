package org.rsinitsyn.dto.request;

import org.rsinitsyn.domain.MatchType;

public record CreateMatchDto(MatchType type,
                             PlayerResultDto player,
                             PlayerResultDto opponentPlayer,
                             String event) {
    public record PlayerResultDto(String name, int score) {
    }
}
