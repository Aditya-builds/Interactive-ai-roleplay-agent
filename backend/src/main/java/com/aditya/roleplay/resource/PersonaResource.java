package com.aditya.roleplay.resource;

import com.aditya.roleplay.model.CreatePersonaRequest;
import com.aditya.roleplay.model.PlayerPersona;
import com.aditya.roleplay.service.PlayerPersonaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/personas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonaResource {

    @Inject
    PlayerPersonaService personaService;

    @GET
    public List<PlayerPersona> listPersonas() {
        return personaService.listPersonas();
    }

    @POST
    public PlayerPersona createPersona(CreatePersonaRequest request) {
        return personaService.createPersona(request);
    }

    @GET
    @Path("/{id}")
    public PlayerPersona getPersona(@PathParam("id") String id) {
        return personaService.requirePersona(id);
    }
}