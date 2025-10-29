package com.softwarecampus.backend.service.impl.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.ApprovalStatus;
import com.softwarecampus.backend.dto.academy.AcademyCreateRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.dto.academy.AcademyUpdateRequest;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.service.academy.AcademyService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class AcademyServiceImpl implements AcademyService {

    private AcademyRepository academyRepository;

    private Academy findAcademyOrThrow(Long id) {
        return academyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Academy not found with id: " + id));
    }

    /**
     *  훈련기관 등록
     */
    @Override
    public AcademyResponse createAcademy(AcademyCreateRequest request) {
        Academy academy = Academy.builder()
                .name(request.getName())
                .address(request.getAddress())
                .business_number(request.getBusinessNumber())
                .email(request.getEmail())
                .isApproved(ApprovalStatus.PENDING)
                .build();

        Academy savedAcademy = academyRepository.save(academy);
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
     * 훈련기관 전체 조회
     */
    @Override
    @Transactional
    public List<AcademyResponse> getAllAcademies() {
        return academyRepository.findAll()
                .stream()
                .map(AcademyResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 훈련기관 정보 수정 (부분/전체)
     */
    @Override
    public AcademyResponse updateAcademy(Long id, AcademyUpdateRequest request) {
        Academy academy = findAcademyOrThrow(id);

        if (request.getName() != null) {
            academy.setName(request.getName());
        }
        if (request.getAddress() != null) {
            academy.setAddress(request.getAddress());
        }
        if (request.getBusinessNumber() != null) {
            academy.setBusiness_number(request.getBusinessNumber());
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
    public void deleteAcademy(Long id) {
        Academy academy = findAcademyOrThrow(id);
        academyRepository.delete(academy);
    }

    /**
     * 학원 승인 처리
     */
    @Override
    public AcademyResponse approveAcademy(Long id) {
        Academy academy = findAcademyOrThrow(id);
        academy.approve();
        return AcademyResponse.from(academy);
    }

}
