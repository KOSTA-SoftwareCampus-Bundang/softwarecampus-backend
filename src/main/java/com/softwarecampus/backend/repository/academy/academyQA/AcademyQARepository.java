package com.softwarecampus.backend.repository.academy.academyQA;

import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademyQARepository extends JpaRepository<AcademyQA, Long> {

    List<AcademyQA> findAllByAcademyId(Long academyId);
}
