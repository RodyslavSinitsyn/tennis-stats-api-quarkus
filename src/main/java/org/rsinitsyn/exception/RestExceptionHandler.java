package org.rsinitsyn.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RestExceptionHandler implements ExceptionMapper<TennisApiException> {
    @Override
    public Response toResponse(TennisApiException e) {
        return Response
                .status(e.getCode())
                .entity(new ErrorDto(e.getMessage(), e.getCause().getMessage()))
                .build();
    }

    public record ErrorDto(String message, String cause) {
    }
}
