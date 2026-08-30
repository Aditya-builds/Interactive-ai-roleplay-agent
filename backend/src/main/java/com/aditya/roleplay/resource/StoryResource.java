package com.aditya.roleplay.resource;

import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.service.StoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/stories")
@Produces(MediaType.APPLICATION_JSON)
public class StoryResource {

    @Inject
    StoryService storyService;

    @GET
    public List<Story> listStories() {
        return storyService.listStories();
    }

    @GET
    @Path("/{id}")
    public Story getStory(@PathParam("id") String id) {
        return storyService.requireStory(id);
    }
}
