package com.aditya.roleplay.resource;

import com.aditya.roleplay.model.CreateStoryRequest;
import com.aditya.roleplay.model.Story;
import com.aditya.roleplay.service.StoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/stories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StoryResource {

    @Inject
    StoryService storyService;

    @GET
    public List<Story> listStories() {
        return storyService.listStories();
    }

    @POST
    public Story createStory(CreateStoryRequest request) {
        return storyService.createStory(request);
    }

    @GET
    @Path("/{id}")
    public Story getStory(@PathParam("id") String id) {
        return storyService.requireStory(id);
    }
}
