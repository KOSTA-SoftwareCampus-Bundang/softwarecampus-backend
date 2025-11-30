package com.softwarecampus.backend.repository.academy.academyQA;

import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademyQARepository extends JpaRepository<AcademyQA, Long> {

    // 검색 및 페이징 지원 (JPQL로 명시적 조건 처리)
    @Query("SELECT qa FROM AcademyQA qa WHERE qa.academy.id = :academyId AND qa.isDeleted = false AND (LOWER(qa.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(qa.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<AcademyQA> searchByAcademyIdAndKeyword(@Param("academyId") Long academyId, @Param("keyword") String keyword,
            Pageable pageable);

    // 페이징 지원 (검색어 없을 때)
    Page<AcademyQA> findByAcademyId(Long academyId, Pageable pageable);
}
