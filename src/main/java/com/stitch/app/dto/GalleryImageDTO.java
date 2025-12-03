package com.stitch.app.dto;

import com.stitch.app.entity.GalleryImage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GalleryImageDTO {
    private Long id;
    private String title;
    private String description;
    private String fileName;
    private String uploadedByEmail;
    private LocalDateTime createdAt;
    private Long likeCount;
    private Boolean likedByCurrentUser;

    public static GalleryImageDTO from(GalleryImage img, long likes, boolean likedByCurrentUser) {
        return GalleryImageDTO.builder()
                .id(img.getId())
                .title(img.getTitle())
                .description(img.getDescription())
                .fileName(img.getFileName())
                .uploadedByEmail(img.getUploadedBy() == null ? null : img.getUploadedBy().getEmail())
                .createdAt(img.getCreatedAt())
                .likeCount(likes)
                .likedByCurrentUser(likedByCurrentUser)
                .build();
    }
}
