package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.ReviewSection;
import com.softwarecampus.backend.domain.course.ReviewSectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewSectionRepository extends JpaRepository<ReviewSection, Long> {

    /**
     * ✅ 특정 과정 내 특정 섹션 타입의 평균 점수
     */
    @Query("SELECT AVG(rs.point) FROM ReviewSection rs " +
            "WHERE rs.review.course.id = :courseId AND rs.sectionType = :sectionType")
    Double findAveragePointByCourseIdAndSectionType(
            @Param("courseId") Long courseId,
            @Param("sectionType") ReviewSectionType sectionType);
}
