package org.rsinitsyn.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.rsinitsyn.dto.request.CreatePlayerDto;
import org.rsinitsyn.exception.TennisApiException;

@Entity
@Table(name = "player")
@AllArgsConstructor
@NoArgsConstructor
public class Player extends PanacheEntity {
    @Column(unique = true)
    public String name;
    public String firstName;
    public String lastName;
    public int age;
    public Instant registrationDate = Instant.now();

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "player", fetch = FetchType.EAGER)
    public Set<MatchResult> matches = new HashSet<>();

    public static Player findByName(String name) {
        return (Player) Player.find("name", name).firstResultOptional()
                .orElseThrow(() -> new TennisApiException("Player 'name' not found:" + name));
    }

    public static Player ofDto(CreatePlayerDto dto) {
        return new Player(
                dto.name(),
                dto.firstName(),
                dto.lastName(),
                dto.age(),
                Instant.now(),
                null
        );
    }
}
