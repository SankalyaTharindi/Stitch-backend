package com.stitch.app.repository;

import com.stitch.app.entity.GalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {
    List<GalleryImage> findAllByOrderByCreatedAtDesc();
}

