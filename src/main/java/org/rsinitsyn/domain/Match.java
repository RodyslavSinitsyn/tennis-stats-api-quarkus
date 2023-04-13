package org.rsinitsyn.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "match")
public class Match extends PanacheEntity {
    @Enumerated(EnumType.STRING)
    public MatchType type = MatchType.SHORT;
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    public Tournament tournament;
}
