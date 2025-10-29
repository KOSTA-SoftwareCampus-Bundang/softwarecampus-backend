package com.softwarecampus.backend.repository.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademyRepository extends JpaRepository<Academy, Long> {
    // 특정 문자열이 포함된 훈련기관 찾기
    List<Academy> findByNameContaining(String name);

}
