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
import javax.ws.rs.core.Response;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.dto.request.BaseStatsFilter;
import org.rsinitsyn.dto.request.CreatePlayerDto;
import org.rsinitsyn.dto.request.PlayerStatsFilters;
import org.rsinitsyn.dto.response.PlayerMatchesDto;
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
    @Path("/stats/{name}/csv")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getStatsByPlayerNameInCsv(@PathParam("name") String name,
                                              @BeanParam PlayerStatsFilters filters) {
        return Response.ok(service.getPlayerStatsCsv(name, filters))
                .header("Content-disposition", "attachment; filename=stats.csv")
                .build();
    }

    @GET
    @Path("/stats/{name}/xlsx")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getStatsByPlayerNameInExcel(@PathParam("name") String name,
                                                @BeanParam BaseStatsFilter filters) {
        return Response.ok(service.getPlayerStatsExcel(name, filters))
                .header("Content-disposition", "attachment; filename=stats.xlsx")
                .build();
    }


    @GET
    @Path("/stats/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public PlayerStatsDto getStatsByPlayerName(@PathParam("name") String name,
                                               @BeanParam PlayerStatsFilters filters) {
        return service.getPlayerStats(name, filters);
    }

    @GET
    @Path("/matches/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public PlayerMatchesDto getMatchesHistoryByPlayerName(@PathParam("name") String name,
                                                          @BeanParam PlayerStatsFilters filters,
                                                          @QueryParam("growSort") boolean growSort) {
        return service.getPlayerMatches(name, filters, growSort, false);
    }

    @GET
    @Path("/matches-formatted/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public PlayerMatchesDto getFormattedMatchesHistoryByPlayerName(@PathParam("name") String name,
                                                                   @BeanParam PlayerStatsFilters filters,
                                                                   @QueryParam("growSort") boolean growSort) {
        return service.getPlayerMatches(name, filters, growSort, true);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Player save(CreatePlayerDto dto) {
        return service.savePlayer(dto);
    }

}