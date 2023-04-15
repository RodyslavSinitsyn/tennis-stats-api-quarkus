package org.rsinitsyn.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TournamentStage {
    FRIENDLY("Товарняк"),
    GROUP("Групповой этап"),
    PLAY_OFF_UPPER_BRACKET("Плей-офф верхняя сетка"),
    PLAY_OFF_LOW_BRACKET("Плей-офф нижняя сетка"),
    FINAL_UPPER_BRACKET("Финал верхней сетки"),
    FINAL_LOWER_BRACKET("Финал нижней сетки"),
    GRAND_FINAL("Гранд-Финал");

    private final String details;
}
