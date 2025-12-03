package com.stitch.app.service;

import com.stitch.app.dto.GalleryImageDTO;
import com.stitch.app.entity.GalleryImage;
import com.stitch.app.entity.GalleryLike;
import com.stitch.app.entity.User;
import com.stitch.app.repository.GalleryImageRepository;
import com.stitch.app.repository.GalleryLikeRepository;
import com.stitch.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryImageRepository galleryImageRepository;
    private final GalleryLikeRepository galleryLikeRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @Transactional
    public GalleryImage uploadImage(MultipartFile file, String title, String description, String uploaderEmail) {
        String storedFile = fileStorageService.storeFile(file);
        User uploader = userRepository.findByEmail(uploaderEmail).orElse(null);

        GalleryImage img = GalleryImage.builder()
                .title(title == null ? "" : title)
                .description(description)
                .fileName(storedFile)
                .uploadedBy(uploader)
                .build();

        return galleryImageRepository.save(img);
    }

    // New: listAll with awareness of current user to compute likedByCurrentUser
    public List<GalleryImageDTO> listAll(String currentUserEmail) {
        User currentUser = null;
        if (currentUserEmail != null) {
            currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        }
        final User finalCurrentUser = currentUser;
        return galleryImageRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(img -> {
                    long likes = galleryLikeRepository.countByImage(img);
                    boolean likedByCurrentUser = false;
                    if (finalCurrentUser != null) {
                        likedByCurrentUser = galleryLikeRepository.findByImageAndUser(img, finalCurrentUser).isPresent();
                    }
                    return GalleryImageDTO.from(img, likes, likedByCurrentUser);
                })
                .collect(Collectors.toList());
    }

    // Keep the original signature for backwards compatibility (delegates to new method with null email)
    public List<GalleryImageDTO> listAll() {
        return listAll(null);
    }

    public GalleryImage getById(Long id) {
        return galleryImageRepository.findById(id).orElseThrow(() -> new RuntimeException("Image not found"));
    }

    @Transactional
    public long toggleLike(Long imageId, String userEmail) {
        GalleryImage img = getById(imageId);
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new RuntimeException("User not found"));
        GalleryLike like = galleryLikeRepository.findByImageAndUser(img, user).orElse(null);
        if (like == null) {
            galleryLikeRepository.save(GalleryLike.builder().image(img).user(user).build());
        } else {
            galleryLikeRepository.delete(like);
        }
        return galleryLikeRepository.countByImage(img);
    }

    // New helper: check if a specific user has liked an image
    public boolean isLikedByUser(Long imageId, String userEmail) {
        if (userEmail == null) return false;
        GalleryImage img = galleryImageRepository.findById(imageId).orElse(null);
        if (img == null) return false;
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return false;
        return galleryLikeRepository.findByImageAndUser(img, user).isPresent();
    }

    @Transactional
    public GalleryImage updateGalleryImage(Long id, String title, String description) {
        GalleryImage img = getById(id);
        if (title != null) img.setTitle(title);
        if (description != null) img.setDescription(description);
        return galleryImageRepository.save(img);
    }

    // New helper: return current like count for an image id
    public long countLikes(Long imageId) {
        GalleryImage img = galleryImageRepository.findById(imageId).orElseThrow(() -> new RuntimeException("Image not found"));
        return galleryLikeRepository.countByImage(img);
    }
}
