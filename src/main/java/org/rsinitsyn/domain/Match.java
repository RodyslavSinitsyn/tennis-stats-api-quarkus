package org.rsinitsyn.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "match")
public class Match extends PanacheEntity {
    @Enumerated(EnumType.STRING)
    public MatchType type = MatchType.SHORT;
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "tournamentId", referencedColumnName = "id")
    public Tournament tournament;
    @Enumerated(EnumType.STRING)
    public TournamentStage stage;
    public LocalDateTime date;
}
