package com.inventory.backend.controller;

import com.inventory.backend.auth.AdminOnly;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serves images from a local folder on the server PC.
 * Folder: Documents\CFT-Electronic\images\
 *
 * Workflow:
 *   1. Copy image files into that folder.
 *   2. When adding a product, pick the filename from the dropdown.
 *   3. App serves it at GET /api/images/serve/{filename}
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Path IMAGE_DIR = Paths.get(
            System.getProperty("user.home"), "Documents", "CFT-Electronic", "images");

    /** Returns the absolute path of the images folder (shown to the user in the UI). */
    @AdminOnly
    @GetMapping("/folder")
    public ResponseEntity<String> folder() {
        return ResponseEntity.ok(IMAGE_DIR.toAbsolutePath().toString());
    }


    /** Lists all image filenames in the folder. */
    @GetMapping("/list")
    public ResponseEntity<List<String>> list() throws IOException {
        Files.createDirectories(IMAGE_DIR);
        try (var stream = Files.list(IMAGE_DIR)) {
            List<String> names = stream
                    .filter(p -> !Files.isDirectory(p))
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.matches("(?i).*\\.(jpg|jpeg|png|gif|webp|avif|bmp|svg)"))
                    .sorted()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(names);
        }
    }

    /**
     * Uploads an image file into the images folder and returns its filename.
     * The browser file picker triggers this — no need to manually copy files.
     */
    private static final java.util.Set<String> ALLOWED_EXT = java.util.Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "avif", "bmp", "svg");

    @AdminOnly
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file type — only image extensions accepted
            String original = file.getOriginalFilename();
            String filename = original != null ? Paths.get(original).getFileName().toString() : "";
            String ext = filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            if (!ALLOWED_EXT.contains(ext)) {
                return ResponseEntity.badRequest().body(Map.of("message", "File type not allowed: " + ext));
            }

            Files.createDirectories(IMAGE_DIR);
            Path dest = IMAGE_DIR.resolve(filename);

            // Stored images keep their exact name and content: products point at them by filename
            if (Files.exists(dest)) {
                if (Arrays.equals(Files.readAllBytes(dest), file.getBytes())) {
                    return ResponseEntity.ok(Map.of("filename", filename));
                }
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",
                        "Ya existe otra imagen llamada " + filename
                                + ". Seleccionala de la lista o sube el archivo con otro nombre."));
            }

            Files.copy(file.getInputStream(), dest);
            return ResponseEntity.ok(Map.of("filename", filename));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    /** Streams an image file from the folder. */
    @GetMapping("/serve/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) throws IOException {
        Files.createDirectories(IMAGE_DIR);
        Path file = IMAGE_DIR.resolve(filename).normalize();

        // Prevent path traversal
        if (!file.startsWith(IMAGE_DIR.toAbsolutePath().normalize())) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) return ResponseEntity.notFound().build();

        String contentType = Files.probeContentType(file);
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }
}
