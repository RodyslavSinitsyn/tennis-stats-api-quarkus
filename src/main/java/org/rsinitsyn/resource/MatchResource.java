package org.rsinitsyn.resource;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.rsinitsyn.domain.MatchPlayer;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.response.MatchRepresentationDto;
import org.rsinitsyn.service.TennisService;

@Path("/match")
public class MatchResource {

    @Inject
    TennisService service;

    @GET
    @Path("/all/beautify")
    @Produces(value = MediaType.APPLICATION_JSON)
    public List<MatchRepresentationDto> getAll() {
        return service.getAllMatchesRepresentations();
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Produces(value = MediaType.APPLICATION_JSON)
    public List<MatchPlayer> save(CreateMatchDto dto) {
        return service.saveMatch(dto);
    }
}
