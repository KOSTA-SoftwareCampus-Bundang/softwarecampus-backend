package com.softwarecampus.backend.repository.academy;

import com.softwarecampus.backend.domain.academy.AcademyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 기관 답변 레포지토리
 */
@Repository
public interface AcademyAnswerRepository extends JpaRepository<AcademyAnswer, Long> {
}
