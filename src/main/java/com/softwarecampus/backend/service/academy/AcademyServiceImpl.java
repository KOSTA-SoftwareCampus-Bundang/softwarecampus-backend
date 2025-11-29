package com.softwarecampus.backend.service.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.AcademyFile;
import com.softwarecampus.backend.domain.academy.ApprovalStatus;
import com.softwarecampus.backend.dto.academy.AcademyCreateRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.dto.academy.AcademyUpdateRequest;
import com.softwarecampus.backend.exception.academy.AcademyErrorCode;
import com.softwarecampus.backend.exception.academy.AcademyException;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.service.user.email.EmailSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademyServiceImpl implements AcademyService {

    private final AcademyRepository academyRepository;
    // 파일 업로드 서비스 (작성자: GitHub Copilot, 작성일: 2025-11-28)
    private final AcademyFileService academyFileService;
    // 이메일 발송 서비스 (작성자: GitHub Copilot, 작성일: 2025-11-28)
    private final EmailSendService emailSendService;

    private Academy findAcademyOrThrow(Long id) {
        return academyRepository.findById(id)
                .orElseThrow(() -> new AcademyException(AcademyErrorCode.ACADEMY_NOT_FOUND));
    }

    /**
     *  훈련기관 등록
     *  수정자: GitHub Copilot
     *  수정일: 2025-11-28
     *  수정 내용: 파일 업로드 기능 추가 (재직증명서)
     *  수정일: 2025-11-29 - 트랜잭션 롤백 시 S3 파일 정리 보상 로직 추가
     */
    @Override
    @Transactional
    public AcademyResponse createAcademy(AcademyCreateRequest request) {
        // 1. Academy 엔티티 생성 및 저장
        Academy academy = Academy.builder()
                .name(request.getName())
                .address(request.getAddress())
                .businessNumber(request.getBusinessNumber())
                .email(request.getEmail())
                .isApproved(ApprovalStatus.PENDING)
                .build();

        Academy savedAcademy = academyRepository.save(academy);
        
        // 2. 파일 업로드 (S3) - 트랜잭션 롤백 시 보상 로직 포함
        List<String> uploadedS3Urls = new ArrayList<>();
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            try {
                for (var file : request.getFiles()) {
                    AcademyFile uploaded = academyFileService.uploadFile(file, savedAcademy.getId());
                    uploadedS3Urls.add(uploaded.getFileUrl());
                }
            } catch (Exception e) {
                // 업로드 중 실패 시 이미 업로드된 S3 파일 정리 (보상 트랜잭션)
                log.warn("파일 업로드 실패 - 이미 업로드된 {} 개 파일 S3에서 삭제 시도", uploadedS3Urls.size());
                for (String s3Url : uploadedS3Urls) {
                    academyFileService.deleteS3FileOnly(s3Url);
                }
                throw e; // 예외 다시 던져서 트랜잭션 롤백 유도
            }
        }
        
        return AcademyResponse.from(savedAcademy);
    }

    /**
     * 훈련기관 단일 조회
     */
    @Override
    @Transactional
    public List<AcademyResponse> searchAcademiesByName(String name) {
        List<Academy> academies = academyRepository.findByNameContaining(name);

        return academies.stream().map(AcademyResponse::from).collect(Collectors.toList());
    }

    /**
     * 훈련기관 전체 조회 (이름만 보여줌)
     */
    @Override
    @Transactional
    public List<AcademyResponse> getAllAcademyNames() {
        return academyRepository.findAll()
                .stream()
                .map(AcademyResponse::from)
                .collect(Collectors.toList());
    }

    /**
     *  훈련기관 상세 정보 조회
     */
    @Override
    public AcademyResponse getAcademyDetails(Long id) {
        Academy academy = findAcademyOrThrow(id);
        return AcademyResponse.from(academy);
    }

    /**
     * 훈련기관 정보 수정 (부분/전체)
     */
    @Override
    @Transactional
    public AcademyResponse updateAcademy(Long id, AcademyUpdateRequest request) {
        Academy academy = findAcademyOrThrow(id);

        if (request.getName() != null) {
            academy.setName(request.getName());
        }
        if (request.getAddress() != null) {
            academy.setAddress(request.getAddress());
        }
        if (request.getBusinessNumber() != null) {
            academy.setBusinessNumber(request.getBusinessNumber());
        }
        if (request.getEmail() != null) {
            academy.setEmail(request.getEmail());
        }
        return AcademyResponse.from(academy);
    }

    /**
     * 훈련기관 삭제
     */
    @Override
    @Transactional
    public void deleteAcademy(Long id) {
        Academy academy = findAcademyOrThrow(id);
        academyRepository.delete(academy);
    }

    /**
     * 학원 승인 처리
     * 수정자: GitHub Copilot
     * 수정일: 2025-11-28
     * 수정 내용: 승인 완료 이메일 발송 기능 추가
     * 수정일: 2025-11-29 - 이메일 발송을 트랜잭션 커밋 후로 분리
     */
    @Override
    @Transactional
    public AcademyResponse approveAcademy(Long id) {
        Academy academy = findAcademyOrThrow(id);
        academy.approve();
        AcademyResponse response = AcademyResponse.from(academy);
        
        // 트랜잭션 커밋 후 이메일 발송 (이메일 실패해도 승인은 완료)
        String email = academy.getEmail();
        String name = academy.getName();
        if (email != null) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            emailSendService.sendAcademyApprovalEmail(email, name);
                        } catch (Exception e) {
                            log.error("기관 승인 이메일 발송 실패 - 기관 ID: {}", id, e);
                        }
                    }
                }
            );
        } else {
            log.warn("기관 ID {}는 이메일 주소가 없어 승인 이메일을 발송하지 않습니다", id);
        }
        
        return response;
    }
    
    /**
     * 기관 거절 처리
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     * 수정일: 2025-11-29 - 이메일 발송을 트랜잭션 커밋 후로 분리
     */
    @Override
    @Transactional
    public AcademyResponse rejectAcademy(Long id, String reason) {
        Academy academy = findAcademyOrThrow(id);
        academy.reject(reason);
        AcademyResponse response = AcademyResponse.from(academy);
        
        // 트랜잭션 커밋 후 이메일 발송 (이메일 실패해도 거절은 완료)
        String email = academy.getEmail();
        String name = academy.getName();
        if (email != null) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            emailSendService.sendAcademyRejectionEmail(email, name, reason);
                        } catch (Exception e) {
                            log.error("기관 거절 이메일 발송 실패 - 기관 ID: {}", id, e);
                        }
                    }
                }
            );
        } else {
            log.warn("기관 ID {}는 이메일 주소가 없어 거절 이메일을 발송하지 않습니다", id);
        }
        
        return response;
    }

}
