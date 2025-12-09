package com.stitch.app.controller;

import com.stitch.app.dto.GalleryImageDTO;
import com.stitch.app.dto.GalleryUpdateRequest;
import com.stitch.app.entity.GalleryImage;
import com.stitch.app.entity.User;
import com.stitch.app.service.FileStorageService;
import com.stitch.app.service.GalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;
    private final FileStorageService fileStorageService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<GalleryImageDTO> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal User admin) {

        GalleryImage img = galleryService.uploadImage(file, title, description, admin.getEmail());
        long likes = galleryService.countLikes(img.getId());
        boolean likedByCurrentUser = false;
        return ResponseEntity.ok(GalleryImageDTO.from(img, likes, likedByCurrentUser));
    }

    @GetMapping
    public ResponseEntity<List<GalleryImageDTO>> listAll(@AuthenticationPrincipal User user) {
        String userEmail = user != null ? user.getEmail() : null;
        Long userId = user != null ? user.getId() : null;
        System.out.println("GET /api/gallery - Authenticated user: " + userEmail + " (ID: " + userId + ")");
        List<GalleryImageDTO> dtos = galleryService.listAll(userEmail);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GalleryImageDTO> getById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        GalleryImage img = galleryService.getById(id);
        long likes = galleryService.countLikes(id);
        boolean likedByCurrentUser = galleryService.isLikedByUser(id, user != null ? user.getEmail() : null);
        return ResponseEntity.ok(GalleryImageDTO.from(img, likes, likedByCurrentUser));
    }

    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Long> toggleLike(@PathVariable Long id, @AuthenticationPrincipal User user) {
        System.out.println("POST /api/gallery/" + id + "/like - Authenticated user: " +
                         user.getEmail() + " (ID: " + user.getId() + ")");
        long likes = galleryService.toggleLike(id, user.getEmail());
        return ResponseEntity.ok(likes);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<GalleryImageDTO> updateGallery(@PathVariable Long id, @RequestBody GalleryUpdateRequest request) {
        var updated = galleryService.updateGalleryImage(id, request.getTitle(), request.getDescription());
        long likes = galleryService.countLikes(id);
        boolean likedByCurrentUser = false;
        return ResponseEntity.ok(GalleryImageDTO.from(updated, likes, likedByCurrentUser));
    }
}
