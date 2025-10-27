package com.softwarecampus.backend.repository.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.AcademyQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 기관 질문 레포지토리
 */
@Repository
public interface AcademyQuestionRepository extends JpaRepository<AcademyQuestion, Long> {
    
    // 기관별 질문 목록
    List<AcademyQuestion> findByAcademyAndIsDeletedFalseOrderByCreatedAtDesc(Academy academy);
    
    // 페이징 처리
    Page<AcademyQuestion> findByAcademyAndIsDeletedFalse(Academy academy, Pageable pageable);
}
