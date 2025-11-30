package com.softwarecampus.backend.repository.academy.academyQA;

import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademyQARepository extends JpaRepository<AcademyQA, Long> {

    // 검색 및 페이징 지원
    Page<AcademyQA> findByAcademyIdAndTitleContainingOrQuestionTextContaining(Long academyId, String titleKeyword,
            String questionKeyword, Pageable pageable);

    // 페이징 지원 (검색어 없을 때)
    Page<AcademyQA> findByAcademyId(Long academyId, Pageable pageable);
}
