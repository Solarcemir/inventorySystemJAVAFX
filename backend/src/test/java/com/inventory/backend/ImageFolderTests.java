package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Stored images are referenced by their filename, so these tests pin down that
 * uploads never rename or replace them. user.home points inside target/ (see pom.xml).
 */
@IsolatedDatabaseTest
class ImageFolderTests {

    @Autowired TestData data;
    @Autowired TestRestTemplate rest;

    private Path folder;
    private String admin;
    private String seller;

    @BeforeAll
    static void refuseToRunOnTheRealFolder() {
        assertThat(System.getProperty("user.home")).contains("test-home");
    }

    @BeforeEach
    void reset() throws IOException {
        data.clear();
        data.user("owner", "owner-pass", "ADMIN");
        data.user("vendor", "vendor-pass", "SELLER");
        admin = login("owner", "owner-pass");
        seller = login("vendor", "vendor-pass");

        folder = Paths.get(System.getProperty("user.home"), "Documents", "CFT-Electronic", "images");
        Files.createDirectories(folder);
    }

    private String uniqueName(String suffix) {
        return UUID.randomUUID() + suffix;
    }

    private String login(String username, String password) {
        return rest.postForEntity("/api/login", Map.of("username", username, "password", password), String.class)
                .getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private ResponseEntity<String> upload(String filename, String content, String cookie) {
        ByteArrayResource file = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add(HttpHeaders.COOKIE, cookie);
        return rest.exchange("/api/images/upload", HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
    }

    private ResponseEntity<byte[]> serve(String filename, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return rest.exchange("/api/images/serve/" + filename, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    private long fileCount() throws IOException {
        try (var files = Files.list(folder)) {
            return files.count();
        }
    }

    @Test
    void aNewImageKeepsItsExactName() throws IOException {
        String name = uniqueName(" Radio Pioneer (2).JPG");

        ResponseEntity<String> response = upload(name, "picture", admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"filename\":\"" + name + "\"");
        assertThat(Files.readString(folder.resolve(name))).isEqualTo("picture");
    }

    @Test
    void uploadingTheSamePictureAgainReusesIt() throws IOException {
        String name = uniqueName("radio.jpg");
        Files.writeString(folder.resolve(name), "picture");
        long before = fileCount();

        ResponseEntity<String> response = upload(name, "picture", admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"filename\":\"" + name + "\"");
        assertThat(fileCount()).isEqualTo(before);
    }

    @Test
    void aDifferentPictureNeverReplacesOrRenamesTheStoredOne() throws IOException {
        String name = uniqueName("radio.jpg");
        Files.writeString(folder.resolve(name), "original");
        long before = fileCount();

        ResponseEntity<String> response = upload(name, "something else", admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains(name);
        assertThat(Files.readString(folder.resolve(name))).isEqualTo("original");
        assertThat(fileCount()).isEqualTo(before);
    }

    @Test
    void sellersCanSeeImagesFromTheFolderButNotUpload() throws IOException {
        String name = uniqueName("radio.jpg");
        Files.writeString(folder.resolve(name), "picture");
        long before = fileCount();

        assertThat(serve(name, seller).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(upload(uniqueName("new.jpg"), "picture", seller).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(fileCount()).isEqualTo(before);
    }

    @Test
    void filesOutsideTheImageTypesAreRejected() throws IOException {
        long before = fileCount();

        assertThat(upload(uniqueName("notes.txt"), "text", admin).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fileCount()).isEqualTo(before);
    }
}
