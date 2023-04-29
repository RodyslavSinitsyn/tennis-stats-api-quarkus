package org.rsinitsyn.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import java.time.Instant;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name = "tournament")
public class Tournament extends PanacheEntity {
    @Column(unique = true)
    public String name;
    public String fullName;
    public String description;
    @Enumerated(EnumType.STRING)
    public TournamentType type;
    public Instant date = Instant.now();

    public static Optional<Tournament> findByName(String name) {
        return find("name", name).firstResultOptional();
    }
}
