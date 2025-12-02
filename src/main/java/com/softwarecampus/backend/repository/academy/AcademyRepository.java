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

    /**
     * 관리자용 기관 검색 (이름, 주소, 승인 상태)
     */
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Academy a " +
            "WHERE a.deletedAt IS NULL " +
            "AND (:status IS NULL OR a.isApproved = :status) " +
            "AND (:keyword IS NULL OR LOWER(a.name) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(a.address) LIKE CONCAT('%', LOWER(:keyword), '%'))")
    org.springframework.data.domain.Page<Academy> searchAcademies(
            @org.springframework.data.repository.query.Param("status") com.softwarecampus.backend.domain.academy.ApprovalStatus status,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);
}
