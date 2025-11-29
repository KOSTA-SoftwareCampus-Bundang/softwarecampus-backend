package com.softwarecampus.backend.repository.academy;

import com.softwarecampus.backend.domain.academy.AcademyFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 기관 첨부파일 Repository
 */
@Repository
public interface AcademyFileRepository extends JpaRepository<AcademyFile, Long> {
    
    /**
     * 특정 기관의 모든 첨부파일 조회
     * @param academyId 기관 ID
     * @return 첨부파일 목록
     */
    List<AcademyFile> findByAcademyId(Long academyId);
    
    /**
     * 특정 기관의 첨부파일 개수 조회
     * @param academyId 기관 ID
     * @return 첨부파일 개수
     */
    long countByAcademyId(Long academyId);
}
