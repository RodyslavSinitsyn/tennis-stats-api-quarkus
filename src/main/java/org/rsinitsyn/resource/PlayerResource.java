package org.rsinitsyn.resource;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.dto.request.CreatePlayerDto;
import org.rsinitsyn.dto.request.PlayerStatsFilters;
import org.rsinitsyn.dto.response.PlayerStatsDto;
import org.rsinitsyn.service.TennisService;

@Path("/player")
public class PlayerResource {

    @Inject
    TennisService service;

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Player> getAll() {
        return Player.listAll();
    }

    @GET
    @Path("/stats/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public PlayerStatsDto getDetailsByName(@PathParam("name") String name,
                                           @BeanParam PlayerStatsFilters filters) {
        return service.getPlayerStats(name, filters);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Player save(CreatePlayerDto dto) {
        return service.savePlayer(dto);
    }

}