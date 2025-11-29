/**
 * 기관 파일 관리 서비스 구현체
 * S3Service를 활용하여 기관 첨부파일 업로드/조회/삭제 기능을 제공합니다.
 */
package com.softwarecampus.backend.service.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.AcademyFile;
import com.softwarecampus.backend.exception.S3UploadException;
import com.softwarecampus.backend.repository.academy.AcademyFileRepository;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Folder;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademyFileServiceImpl implements AcademyFileService {

    private final S3Service s3Service;
    private final AcademyFileRepository academyFileRepository;
    private final AcademyRepository academyRepository;

    /**
     * 기관 파일을 S3에 업로드하고 메타데이터를 DB에 저장합니다.
     *
     * @param file 업로드할 파일
     * @param academyId 기관 ID
     * @return S3에 저장된 파일 메타데이터 엔티티
     * @throws IllegalArgumentException academyId에 해당하는 기관이 존재하지 않는 경우
     * @throws S3UploadException S3 업로드 실패 시
     */
    @Override
    @Transactional
    public AcademyFile uploadFile(MultipartFile file, Long academyId) {
        log.info("Starting file upload for academy ID: {}, filename: {}", academyId, file.getOriginalFilename());

        // 1. Academy 존재 확인
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new IllegalArgumentException("기관을 찾을 수 없습니다. ID: " + academyId));

        // 2. S3에 파일 업로드 (파일 검증 및 보안 체크는 S3Service에서 수행)
        String folder = String.format("%s/%d", S3Folder.ACADEMY.getPath(), academyId);
        String fileUrl = s3Service.uploadFile(file, folder, FileType.FileTypeEnum.ACADEMY_FILE);

        // 3. S3 키 생성 (URL에서 bucket URL 부분 제거)
        String s3Key = extractS3KeyFromUrl(fileUrl, folder);

        // 4. 메타데이터 엔티티 생성
        AcademyFile academyFile = AcademyFile.builder()
                .academy(academy)
                .originalFileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .s3Key(s3Key)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();

        // 5. DB 저장
        AcademyFile savedFile = academyFileRepository.save(academyFile);
        log.info("File uploaded successfully. File ID: {}, S3 key: {}", savedFile.getId(), s3Key);

        return savedFile;
    }

    /**
     * 파일 ID로 Presigned URL을 생성합니다.
     * 보안을 위해 1시간 동안만 유효한 임시 다운로드 링크를 제공합니다.
     *
     * @param academyId 기관 ID (파일 소속 검증용)
     * @param fileId 파일 ID
     * @return Presigned URL (1시간 유효)
     * @throws IllegalArgumentException fileId에 해당하는 파일이 존재하지 않거나, 해당 기관에 속하지 않는 경우
     */
    @Override
    public String getFileUrl(Long academyId, Long fileId) {
        log.debug("Generating presigned URL for academy ID: {}, file ID: {}", academyId, fileId);

        // 1. 파일 메타데이터 조회
        AcademyFile academyFile = academyFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다. ID: " + fileId));

        // 2. 파일이 해당 기관에 속하는지 검증 (보안)
        if (!academyFile.getAcademy().getId().equals(academyId)) {
            log.warn("파일 접근 권한 없음: academyId={}, fileId={}, 실제 소속 기관={}", 
                academyId, fileId, academyFile.getAcademy().getId());
            throw new IllegalArgumentException("해당 기관의 파일이 아닙니다.");
        }

        // 3. Presigned URL 생성 (1시간 유효)
        String presignedUrl = s3Service.generatePresignedUrl(
                academyFile.getS3Key(),
                Duration.ofHours(1)
        );

        log.debug("Presigned URL generated for file ID: {}", fileId);
        return presignedUrl;
    }

    /**
     * 특정 기관의 모든 파일을 S3와 DB에서 삭제합니다.
     * 트랜잭션 내에서 실행되며, 실패 시 롤백됩니다.
     *
     * @param academyId 기관 ID
     */
    @Override
    @Transactional
    public void deleteAllFilesByAcademyId(Long academyId) {
        log.info("Deleting all files for academy ID: {}", academyId);

        // 1. 기관의 모든 파일 조회
        List<AcademyFile> files = academyFileRepository.findByAcademyId(academyId);

        if (files.isEmpty()) {
            log.info("No files found for academy ID: {}", academyId);
            return;
        }

        // 2. 각 파일을 S3에서 삭제 후 DB에서 삭제
        for (AcademyFile file : files) {
            // S3 파일 삭제 (최대 3회 재시도)
            deleteS3FileWithRetry(file.getFileUrl(), file.getS3Key(), 3);

            // DB 메타데이터 삭제
            academyFileRepository.delete(file);
        }

        log.info("Deleted {} files for academy ID: {}", files.size(), academyId);
    }

    /**
     * S3 파일 삭제 (재시도 로직 포함)
     * 
     * @param fileUrl S3 파일 URL
     * @param s3Key S3 키 (로깅용)
     * @param maxRetries 최대 재시도 횟수
     */
    private void deleteS3FileWithRetry(String fileUrl, String s3Key, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                s3Service.deleteFile(fileUrl);
                log.debug("S3 file deleted: {}", s3Key);
                return; // 성공 시 종료
            } catch (S3UploadException e) {
                attempt++;
                if (attempt < maxRetries) {
                    log.warn("S3 삭제 실패 (시도 {}/{}): {}, 재시도 중...", attempt, maxRetries, s3Key);
                    try {
                        Thread.sleep(500 * attempt); // 백오프: 500ms, 1000ms, ...
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // 최대 재시도 후에도 실패 - 로그 남기고 DB 삭제는 진행
                    log.error("S3 삭제 최종 실패 ({}회 시도): {} - 고아 파일 발생 가능", maxRetries, s3Key, e);
                }
            }
        }
    }

    /**
     * S3 URL에서 S3 키를 추출합니다.
     * 예: https://bucket.s3.region.amazonaws.com/academy/123/uuid-file.pdf → academy/123/uuid-file.pdf
     *
     * @param fileUrl S3 파일 URL
     * @param folder 폴더 경로 (academy/123)
     * @return S3 키
     */
    private String extractS3KeyFromUrl(String fileUrl, String folder) {
        // URL 형식: https://bucket.s3.region.amazonaws.com/{folder}/{filename}
        int folderIndex = fileUrl.indexOf(folder);
        if (folderIndex == -1) {
            throw new IllegalStateException("URL에서 S3 키를 추출할 수 없습니다: " + fileUrl);
        }
        return fileUrl.substring(folderIndex);
    }
}
