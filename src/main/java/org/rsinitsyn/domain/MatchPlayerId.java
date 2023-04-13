package org.rsinitsyn.domain;

import java.io.Serializable;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class MatchPlayerId implements Serializable {
    public Long match;
    public Long player;
}
