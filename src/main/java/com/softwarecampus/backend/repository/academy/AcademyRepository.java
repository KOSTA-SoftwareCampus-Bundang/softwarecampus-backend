package com.softwarecampus.backend.repository.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademyRepository extends JpaRepository<Academy, Long> {
    // 특정 문자열이 포함된 훈련기관 찾기
    List<Academy> findByNameContaining(String name);

    // ID로 훈련기관 조회 (삭제된 기관 제외)
    java.util.Optional<Academy> findByIdAndDeletedAtIsNull(Long id);

    // ID로 훈련기관 상세 조회 (삭제된 기관 제외, 파일 연관 엔티티 함께 로딩)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "files" })
    java.util.Optional<Academy> findWithFilesByIdAndDeletedAtIsNull(Long id);
}
