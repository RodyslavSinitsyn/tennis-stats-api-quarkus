package org.rsinitsyn.resource;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.rsinitsyn.domain.Match;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.response.MatchRecordsDto;
import org.rsinitsyn.service.TennisService;

@Path("/match")
public class MatchResource {

    @Inject
    TennisService service;

    @GET
    @Path("/all/beautify")
    @Produces(value = MediaType.APPLICATION_JSON)
    public List<String> getAll() {
        return service.getAllMatchesRepresentations();
    }

    @GET
    @Path("/records")
    @Produces(value = MediaType.APPLICATION_JSON)
    public MatchRecordsDto getRecords(@QueryParam("playerName") String playerName) {
        return service.getRecords(playerName);
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Produces(value = MediaType.APPLICATION_JSON)
    public Match save(CreateMatchDto dto) {
        return service.saveMatch(dto);
    }
}
