package org.rsinitsyn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchRepresentationDto {
    private String representation;
    private String tournament;
}
