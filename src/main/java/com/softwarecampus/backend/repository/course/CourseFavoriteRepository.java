package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과정 즐겨찾기 레포지토리
 */
@Repository
public interface CourseFavoriteRepository extends JpaRepository<CourseFavorite, Long> {
}
