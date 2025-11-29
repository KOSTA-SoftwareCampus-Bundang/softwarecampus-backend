package com.softwarecampus.backend.service.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.ApprovalStatus;
import com.softwarecampus.backend.dto.academy.AcademyCreateRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.dto.academy.AcademyUpdateRequest;
import com.softwarecampus.backend.exception.academy.AcademyErrorCode;
import com.softwarecampus.backend.exception.academy.AcademyException;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.service.user.email.EmailSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
        
        // 2. 파일 업로드 (S3) - 작성자: GitHub Copilot, 작성일: 2025-11-28
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            for (var file : request.getFiles()) {
                academyFileService.uploadFile(file, savedAcademy.getId());
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
     */
    @Override
    @Transactional
    public AcademyResponse approveAcademy(Long id) {
        Academy academy = findAcademyOrThrow(id);
        academy.approve();
        
        // 승인 완료 이메일 발송 (작성자: GitHub Copilot, 작성일: 2025-11-28)
        emailSendService.sendAcademyApprovalEmail(
            academy.getEmail(),
            academy.getName()
        );
        
        return AcademyResponse.from(academy);
    }
    
    /**
     * 기관 거절 처리
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @Override
    @Transactional
    public AcademyResponse rejectAcademy(Long id, String reason) {
        Academy academy = findAcademyOrThrow(id);
        academy.reject(reason);
        
        // 거절 이메일 발송 (작성자: GitHub Copilot, 작성일: 2025-11-28)
        emailSendService.sendAcademyRejectionEmail(
            academy.getEmail(),
            academy.getName(),
            reason
        );
        
        return AcademyResponse.from(academy);
    }

}
