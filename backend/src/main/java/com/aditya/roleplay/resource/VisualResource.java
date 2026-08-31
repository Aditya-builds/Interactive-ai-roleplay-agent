package com.aditya.roleplay.resource;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.visual.GeneratedSceneImage;
import com.aditya.roleplay.model.visual.reference.CharacterReferenceLibrarySummary;
import com.aditya.roleplay.visual.VisualImageStorageService;
import com.aditya.roleplay.visual.reference.CharacterReferenceLibraryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;

@Path("/api")
public class VisualResource {

    @Inject
    VisualImageStorageService imageStorageService;

    @Inject
    CharacterReferenceLibraryService referenceLibraryService;

    @GET
    @Path("/scene-images/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public GeneratedSceneImage getSceneImage(@PathParam("id") String imageId) {
        return imageStorageService.loadMetadata(imageId)
                .orElseThrow(() -> new RoleplayException(
                        "Scene image not found: " + imageId, "SCENE_IMAGE_NOT_FOUND", 404));
    }

    @GET
    @Path("/scene-images/{id}/content")
    public Response getSceneImageContent(@PathParam("id") String imageId) throws IOException {
        java.util.Optional<java.nio.file.Path> imagePath = imageStorageService.loadImageFile(imageId);
        if (imagePath.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        java.nio.file.Path file = imagePath.get();
        String mimeType = Files.probeContentType(file);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        return Response.ok(Files.readAllBytes(file)).type(mimeType).build();
    }

    @GET
    @Path("/visuals/references/{characterId}")
    public Response getCanonicalReference(@PathParam("characterId") String characterId) throws IOException {
        java.nio.file.Path reference = imageStorageService.canonicalReferencePath(characterId);
        if (!Files.exists(reference)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String mimeType = Files.probeContentType(reference);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        return Response.ok(Files.readAllBytes(reference))
                .type(mimeType)
                .header("Cache-Control", "no-cache, must-revalidate")
                .build();
    }

    @GET
    @Path("/visuals/references/{characterId}/library")
    @Produces(MediaType.APPLICATION_JSON)
    public CharacterReferenceLibrarySummary getReferenceLibrary(@PathParam("characterId") String characterId) {
        return referenceLibraryService.summarize(characterId);
    }

    @GET
    @Path("/visuals/references/{characterId}/images/{referenceId}")
    public Response getLibraryReferenceImage(
            @PathParam("characterId") String characterId,
            @PathParam("referenceId") String referenceId) throws IOException {
        java.util.Optional<java.nio.file.Path> imagePath =
                imageStorageService.loadCharacterReferenceImage(characterId, referenceId);
        if (imagePath.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        java.nio.file.Path file = imagePath.get();
        String mimeType = Files.probeContentType(file);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        return Response.ok(Files.readAllBytes(file))
                .type(mimeType)
                .header("Cache-Control", "no-cache, must-revalidate")
                .build();
    }
}
