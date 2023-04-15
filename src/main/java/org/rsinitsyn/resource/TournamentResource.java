package org.rsinitsyn.resource;

import java.util.List;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.dto.response.TournamentHistoryDto;
import org.rsinitsyn.service.TennisService;

@Path("/tournament")
public class TournamentResource {

    @Inject
    TennisService tennisService;

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tournament> getAll() {
        return Tournament.listAll();
    }

    @GET
    @Path("/history/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TournamentHistoryDto getTournamentHistory(@PathParam("id") Long id) {
        return tennisService.getTournamentHistory(id);
    }

    @Transactional
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Tournament save(@QueryParam(value = "name") String name,
                           @QueryParam(value = "fullname") String fullname,
                           @QueryParam(value = "description") String description) {
        Tournament tournament = new Tournament();
        tournament.name = name;
        tournament.fullName = fullname;
        tournament.description = description;
        tournament.persist();
        return tournament;
    }
}
