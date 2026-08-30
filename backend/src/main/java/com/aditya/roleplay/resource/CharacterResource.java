package com.aditya.roleplay.resource;

import com.aditya.roleplay.model.CharacterDetailResponse;
import com.aditya.roleplay.model.RoleplayCharacter;
import com.aditya.roleplay.service.CharacterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/characters")
@Produces(MediaType.APPLICATION_JSON)
public class CharacterResource {

    @Inject
    CharacterService characterService;

    @GET
    public List<RoleplayCharacter> listCharacters() {
        return characterService.listCharacters();
    }

    @GET
    @Path("/{id}")
    public CharacterDetailResponse getCharacter(@PathParam("id") String id) {
        return characterService.getCharacterDetail(id);
    }
}
