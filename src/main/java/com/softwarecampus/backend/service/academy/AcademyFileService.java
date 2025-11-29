package com.softwarecampus.backend.service.academy;

import com.softwarecampus.backend.domain.academy.AcademyFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 기관 첨부파일 관리 Service 인터페이스
 * AWS S3를 활용한 파일 업로드/다운로드/삭제 기능 제공
 */
public interface AcademyFileService {
    
    /**
     * 파일을 S3에 업로드하고 메타데이터를 DB에 저장
     * 
     * @param file 업로드할 파일
     * @param academyId 소속 기관 ID
     * @return 저장된 파일 메타데이터
     * @throws com.softwarecampus.backend.exception.academy.AcademyNotFoundException 기관을 찾을 수 없는 경우
     * @throws com.softwarecampus.backend.exception.file.InvalidFileException 파일 검증 실패
     * @throws com.softwarecampus.backend.exception.file.FileStorageException S3 업로드 실패
     */
    AcademyFile uploadFile(MultipartFile file, Long academyId);
    
    /**
     * 파일 접근을 위한 S3 Presigned URL 생성
     * 
     * @param academyId 기관 ID (파일 소속 검증용)
     * @param fileId 파일 ID
     * @return S3 Presigned URL (1시간 유효)
     * @throws com.softwarecampus.backend.exception.file.FileNotFoundException 파일을 찾을 수 없는 경우
     * @throws IllegalArgumentException 파일이 해당 기관에 속하지 않는 경우
     */
    String getFileUrl(Long academyId, Long fileId);
    
    /**
     * 특정 기관의 모든 첨부파일을 S3와 DB에서 삭제
     * 
     * @param academyId 기관 ID
     */
    void deleteAllFilesByAcademyId(Long academyId);
    
    /**
     * S3 파일만 삭제 (DB 메타데이터는 유지)
     * 트랜잭션 롤백 시 보상 로직용
     * 
     * @param s3Url S3 파일 URL
     */
    void deleteS3FileOnly(String s3Url);
}
