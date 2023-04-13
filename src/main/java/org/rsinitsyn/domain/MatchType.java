package org.rsinitsyn.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MatchType {
    SHORT(11),
    LONG(21);
    public final int points;
}
