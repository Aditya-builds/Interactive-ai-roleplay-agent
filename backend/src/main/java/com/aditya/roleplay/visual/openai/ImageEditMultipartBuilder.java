package com.aditya.roleplay.visual.openai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ImageEditMultipartBuilder {

    private ImageEditMultipartBuilder() {
    }

    static byte[] build(String boundary, List<Path> imagePaths, Map<String, String> fields, String imageFieldName) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (Path imagePath : imagePaths) {
            if (!Files.exists(imagePath)) {
                continue;
            }
            byte[] imageBytes = Files.readAllBytes(imagePath);
            String fileName = imagePath.getFileName().toString();
            String contentType = probeContentType(fileName);
            writeFilePart(output, boundary, imageFieldName, fileName, contentType, imageBytes);
        }

        for (Map.Entry<String, String> field : fields.entrySet()) {
            writeTextPart(output, boundary, field.getKey(), field.getValue());
        }

        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private static void writeFilePart(
            ByteArrayOutputStream output,
            String boundary,
            String name,
            String fileName,
            String contentType,
            byte[] bytes) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeTextPart(
            ByteArrayOutputStream output,
            String boundary,
            String name,
            String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String probeContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    static Map<String, String> orderedFields(String model, String prompt, String size, boolean gptImage) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", model);
        fields.put("prompt", prompt);
        fields.put("n", "1");
        fields.put("size", size);
        if (gptImage) {
            fields.put("quality", "high");
            fields.put("output_format", "png");
        }
        return fields;
    }
}
