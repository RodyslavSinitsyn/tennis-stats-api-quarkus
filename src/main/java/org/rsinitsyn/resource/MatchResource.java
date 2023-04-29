package org.rsinitsyn.resource;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.rsinitsyn.domain.Match;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.dto.request.BaseFilter;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.request.ImportSingleMatchesDto;
import org.rsinitsyn.dto.response.MatchPredictionResponse;
import org.rsinitsyn.dto.response.RatingsResponse;
import org.rsinitsyn.dto.response.RecordsResponse;
import org.rsinitsyn.service.ImportService;
import org.rsinitsyn.service.TennisService;

@Path("/match")
public class MatchResource {

    @Inject
    TennisService service;
    @Inject
    ImportService importService;

    @GET
    @Path("/all")
    @Produces(value = MediaType.APPLICATION_JSON)
    public List<String> getAll() {
        return service.getAllMatchesRepresentations();
    }

    @GET
    @Path("/records")
    @Produces(value = MediaType.APPLICATION_JSON)
    public RecordsResponse getRecords() {
        return service.getRecords();
    }

    @GET
    @Path("/ratings")
    @Produces(value = MediaType.APPLICATION_JSON)
    public RatingsResponse getRatings(@BeanParam BaseFilter filter) {
        return service.getRatings(filter);
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Produces(value = MediaType.APPLICATION_JSON)
    public Match save(CreateMatchDto dto) {
        return service.saveMatch(dto);
    }

    @POST
    @Produces(value = MediaType.APPLICATION_JSON)
    @Path("/predict")
    public MatchPredictionResponse predict(@QueryParam("name") String name,
                                           @QueryParam("opponent") String opponent,
                                           @QueryParam("type") Optional<MatchType> type) {
        return service.predictMatchWinner(name, opponent, type.orElse(MatchType.SHORT));
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Produces(value = MediaType.APPLICATION_JSON)
    @Path("/import/single")
    public Response importSingleMatches(ImportSingleMatchesDto dto) {
        return Response.status(201)
                .entity(importService.importSingleMatches(dto))
                .build();
    }
}
