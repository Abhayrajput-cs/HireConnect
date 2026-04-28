package com.hireconnect.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireconnect.web.domain.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserProfileIdAndJobId(Integer userProfileId, Integer jobId);

    List<Bookmark> findByUserProfileIdOrderByCreatedAtDesc(Integer userProfileId);
}
