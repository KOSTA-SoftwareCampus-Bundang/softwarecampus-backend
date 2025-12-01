package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseFavorite;
import com.softwarecampus.backend.dto.course.CourseFavoriteResponseDTO;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.repository.course.CourseFavoriteRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseFavoriteServiceImpl implements CourseFavoriteService {

    private final CourseFavoriteRepository favoriteRepository;
    private final CourseRepository courseRepository;
    private final AccountRepository accountRepository;

    /**
     * 찜하기 추가 (idempotent - 동시성 안전)
     * 
     * 전략: Check-then-Act + Exception Handling
     * 1. 기존 데이터 조회 (Hard Delete이므로 존재하면 무조건 활성 상태)
     * 2. 존재하면: 이미 찜한 상태이므로 무시
     * 3. 없으면: 신규 생성 (save)
     * 4. 동시성 문제로 중복 insert 발생 시 예외 처리
     */
    @Override
    @Transactional
    public void addFavorite(Long accountId, Long courseId) {
        log.info("찜하기 요청 시작 - accountId: {}, courseId: {}", accountId, courseId);

        // 1. 먼저 조회하여 존재 여부 확인
        boolean exists = favoriteRepository.existsByAccount_IdAndCourse_Id(accountId, courseId);
        log.info("찜하기 중복 체크 결과 - exists: {}", exists);

        if (exists) {
            log.info("이미 찜한 상태이므로 무시합니다. - accountId: {}, courseId: {}", accountId, courseId);
            return;
        }

        try {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다."));
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 사용자가 존재하지 않습니다."));

            CourseFavorite favorite = CourseFavorite.builder()
                    .account(account)
                    .course(course)
                    .build();
            favoriteRepository.save(favorite);
            log.info("찜하기 신규 생성 완료 (save 호출됨) - accountId: {}, courseId: {}", accountId, courseId);

        } catch (DataIntegrityViolationException e) {
            // 동시성 문제로 인한 중복 발생 시 무시 (idempotent)
            if (isUniqueConstraintViolation(e)) {
                log.info("찜하기 중복 요청 무시 (unique constraint) - accountId: {}, courseId: {}", accountId, courseId);
            } else {
                log.error("찜하기 실패 - 데이터 무결성 위반 - accountId: {}, courseId: {}, cause: {}",
                        accountId, courseId, e.getRootCause(), e);
                throw e;
            }
        }
    }

    /**
     * DataIntegrityViolationException이 unique constraint 위반인지 확인
     */
    private boolean isUniqueConstraintViolation(DataIntegrityViolationException e) {
        Throwable rootCause = e.getRootCause();
        if (rootCause == null)
            return false;

        String message = rootCause.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            // MySQL: Duplicate entry ... for key ...
            return lowerMessage.contains("duplicate") || lowerMessage.contains("unique");
        }
        return false;
    }

    /**
     * 찜하기 삭제 (idempotent)
     * Hard Delete: DB에서 레코드를 물리적으로 삭제
     */
    @Override
    @Transactional
    public void removeFavorite(Long accountId, Long courseId) {
        log.info("찜하기 삭제 요청 - accountId: {}, courseId: {}", accountId, courseId);
        Optional<CourseFavorite> existing = favoriteRepository.findByAccount_IdAndCourse_Id(accountId, courseId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            log.info("찜하기 삭제 완료 (Hard Delete) - accountId: {}, courseId: {}", accountId, courseId);
        } else {
            log.info("삭제할 찜하기 데이터가 없습니다. - accountId: {}, courseId: {}", accountId, courseId);
        }
    }

    /**
     * 찜 목록 조회
     */
    @Override
    public List<CourseFavoriteResponseDTO> getFavorites(Long accountId) {
        return favoriteRepository.findByAccount_Id(accountId)
                .stream()
                .map(f -> new CourseFavoriteResponseDTO(
                        f.getCourse().getId(),
                        f.getCourse().getName(),
                        true))
                .toList();
    }

    /**
     * 찜 여부 확인
     */
    @Override
    public boolean isFavorite(Long accountId, Long courseId) {
        boolean result = favoriteRepository.existsByAccount_IdAndCourse_Id(accountId, courseId);
        log.info("찜 여부 확인 - accountId: {}, courseId: {}, result: {}", accountId, courseId, result);
        return result;
    }
}
