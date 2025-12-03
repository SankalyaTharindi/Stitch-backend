package com.stitch.app.repository;

import com.stitch.app.entity.GalleryLike;
import com.stitch.app.entity.GalleryImage;
import com.stitch.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GalleryLikeRepository extends JpaRepository<GalleryLike, Long> {
    Optional<GalleryLike> findByImageAndUser(GalleryImage image, User user);
    long countByImage(GalleryImage image);
}

