package com.aditya.roleplay.resource;

import com.aditya.roleplay.model.Conversation;
import com.aditya.roleplay.model.ConversationSummary;
import com.aditya.roleplay.model.CreateConversationRequest;
import com.aditya.roleplay.model.RegenerateMessageRequest;
import com.aditya.roleplay.model.SendMessageRequest;
import com.aditya.roleplay.model.SendMessageResponse;
import com.aditya.roleplay.model.visual.GenerateSceneImageResponse;
import com.aditya.roleplay.service.ConversationService;
import com.aditya.roleplay.service.RoleplayService;
import com.aditya.roleplay.visual.SceneImageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConversationResource {

    @Inject
    ConversationService conversationService;

    @Inject
    RoleplayService roleplayService;

    @Inject
    SceneImageService sceneImageService;

    @GET
    public List<ConversationSummary> listConversations(@QueryParam("characterId") String characterId) {
        return conversationService.listConversations(characterId);
    }

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
    public SendMessageResponse sendMessage(
            @PathParam("id") String id,
            SendMessageRequest request,
            @Context HttpHeaders headers) {
        String userApiKey = headers.getHeaderString("X-LLM-Api-Key");
        return roleplayService.processTurn(id, request.content(), userApiKey, request.replyLength());
    }

    @POST
    @Path("/{id}/messages/regenerate")
    public SendMessageResponse regenerateMessage(
            @PathParam("id") String id,
            RegenerateMessageRequest request,
            @Context HttpHeaders headers) {
        String userApiKey = headers.getHeaderString("X-LLM-Api-Key");
        String replyLength = request != null ? request.replyLength() : null;
        return roleplayService.regenerateLastTurn(id, replyLength, userApiKey);
    }

    @POST
    @Path("/{id}/scene-images")
    public GenerateSceneImageResponse generateSceneImage(
            @PathParam("id") String id,
            @Context HttpHeaders headers) {
        String userApiKey = headers.getHeaderString("X-LLM-Api-Key");
        return sceneImageService.generateForConversation(id, userApiKey);
    }

    @DELETE
    @Path("/{id}")
    public void deleteConversation(@PathParam("id") String id) {
        conversationService.deleteConversation(id);
    }
}
