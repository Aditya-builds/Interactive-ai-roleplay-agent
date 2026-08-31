package com.aditya.roleplay.resource;

import com.aditya.roleplay.exception.RoleplayException;
import com.aditya.roleplay.model.UploadImageResponse;
import com.aditya.roleplay.visual.ContentImageStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

@Path("/api/content")
public class ContentResource {

    @Inject
    ContentImageStorageService contentImageStorageService;

    @POST
    @Path("/images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public UploadImageResponse uploadImage(
            @org.jboss.resteasy.reactive.RestForm("file") FileUpload file,
            @org.jboss.resteasy.reactive.RestForm("kind") String kind) throws IOException {
        if (file == null || file.uploadedFile() == null) {
            throw new RoleplayException("Image file is required", "INVALID_REQUEST", 400);
        }
        java.nio.file.Path uploaded = file.uploadedFile();
        byte[] bytes = Files.readAllBytes(uploaded);
        if (bytes.length == 0) {
            throw new RoleplayException("Image file is empty", "INVALID_REQUEST", 400);
        }
        String imageUrl = contentImageStorageService.saveUploadedImage(
                kind,
                bytes,
                file.fileName());
        return new UploadImageResponse(imageUrl);
    }

    @GET
    @Path("/images/{kind}/{filename}")
    public Response getImage(@PathParam("kind") String kind, @PathParam("filename") String filename)
            throws IOException {
        java.nio.file.Path imagePath = contentImageStorageService.resolveImagePath(kind, filename);
        if (!Files.exists(imagePath)) {
            return Response.status(404).build();
        }
        String contentType = probeContentType(filename);
        return Response.ok(Files.readAllBytes(imagePath)).type(contentType).build();
    }

    private static String probeContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }
}
