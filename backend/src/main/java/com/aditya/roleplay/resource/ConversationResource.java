package com.aditya.roleplay.resource;

import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.CreateConversationRequest;
import com.aditya.roleplay.model.SendMessageRequest;
import com.aditya.roleplay.model.SendMessageResponse;
import com.aditya.roleplay.service.ConversationService;
import com.aditya.roleplay.service.RoleplayService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConversationResource {

    @Inject
    ConversationService conversationService;

    @Inject
    RoleplayService roleplayService;

    @POST
    public Conversation createConversation(CreateConversationRequest request) {
        return conversationService.create(request);
    }

    @GET
    @Path("/{id}")
    public Conversation getConversation(@PathParam("id") String id) {
        return conversationService.getConversation(id);
    }

    @POST
    @Path("/{id}/messages")
    public SendMessageResponse sendMessage(@PathParam("id") String id, SendMessageRequest request) {
        return roleplayService.processTurn(id, request.content());
    }

    @DELETE
    @Path("/{id}")
    public void deleteConversation(@PathParam("id") String id) {
        conversationService.deleteConversation(id);
    }
}
