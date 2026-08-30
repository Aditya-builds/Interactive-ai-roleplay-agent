package com.aditya.roleplay.exception;

import com.aditya.roleplay.model.ApiError;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RoleplayExceptionMapper implements ExceptionMapper<RoleplayException> {

    @Override
    public Response toResponse(RoleplayException exception) {
        ApiError error = new ApiError(exception.getMessage(), exception.getCode());
        return Response.status(exception.getStatus()).entity(error).build();
    }
}
