package org.rsinitsyn.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.rsinitsyn.dto.request.CreatePlayerDto;

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
    public LocalDateTime registrationDate = LocalDateTime.now();

    public static Player ofDto(CreatePlayerDto dto) {
        return new Player(
                dto.name(),
                dto.firstName(),
                dto.lastName(),
                dto.age(),
                LocalDateTime.now()
        );
    }
}
