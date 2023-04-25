package org.rsinitsyn.resource;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.rsinitsyn.domain.Match;
import org.rsinitsyn.dto.request.BaseFilter;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.response.RatingsResponse;
import org.rsinitsyn.dto.response.RecordsResponse;
import org.rsinitsyn.service.TennisService;

@Path("/match")
public class MatchResource {

    @Inject
    TennisService service;

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
}
