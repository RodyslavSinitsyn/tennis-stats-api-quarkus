package org.rsinitsyn.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(name = "match_result")
@IdClass(MatchResultId.class)
@Setter
@Getter
public class MatchResult {
    @Id
    @ManyToOne
    @JoinColumn(name = "matchid", referencedColumnName = "id")
    private Match match;
    @Id
    @ManyToOne
    @JoinColumn(name = "playerid", referencedColumnName = "id")
    private Player player;
    private int scored;
    private int missed;
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean winner;
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean extraRound;
    @NotFound(action = NotFoundAction.EXCEPTION)
    @ManyToOne
    @JoinColumn(name = "opponentid", referencedColumnName = "id", nullable = false)
    private Player opponent;
}
