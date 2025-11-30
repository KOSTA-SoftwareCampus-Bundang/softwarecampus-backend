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

import java.sql.SQLException;
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
     * TOCTOU 경쟁 조건 해결:
     * - check-then-create 대신 save 후 unique constraint 예외 처리
     * - 동시 요청 시 하나는 성공, 나머지는 예외 발생하지만 모두 정상 처리
     * 
     * Unique constraint만 선택적으로 처리:
     * - account_id + course_id unique constraint 위반: idempotent 처리
     * - 기타 데이터 무결성 위반 (NOT NULL, CHECK 등): 예외 전파
     */
    @Override
    @Transactional
    public void addFavorite(Long accountId, Long courseId) {
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
            
        } catch (DataIntegrityViolationException e) {
            // Root cause 확인: unique constraint 위반인지 검증
            if (isUniqueConstraintViolation(e, accountId, courseId)) {
                // account_id + course_id unique constraint 위반 = 이미 찜한 상태 → 정상 처리 (idempotent)
                log.debug("찜하기 중복 요청 무시 (unique constraint) - accountId: {}, courseId: {}", accountId, courseId);
                // no-op: 이미 존재하므로 아무 작업도 하지 않음
            } else {
                // 다른 종류의 데이터 무결성 위반 (NOT NULL, FK, CHECK 등)
                log.error("찜하기 실패 - 데이터 무결성 위반 - accountId: {}, courseId: {}, cause: {}", 
                          accountId, courseId, e.getRootCause(), e);
                throw e; // 예외 전파
            }
        }
    }

    /**
     * DataIntegrityViolationException이 account_id + course_id unique constraint 위반인지 확인
     * 
     * @param e DataIntegrityViolationException
     * @param accountId 계정 ID (로깅용)
     * @param courseId 과정 ID (로깅용)
     * @return unique constraint 위반 여부
     */
    private boolean isUniqueConstraintViolation(DataIntegrityViolationException e, Long accountId, Long courseId) {
        Throwable rootCause = e.getRootCause();
        
        if (rootCause == null) {
            return false;
        }

        // Hibernate ConstraintViolationException 확인
        if (rootCause instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) rootCause;
            String constraintName = cve.getConstraintName();
            
            // Constraint 이름 확인 (MySQL: UK_*, PostgreSQL: course_favorite_account_id_course_id_key 등)
            // 또는 컬럼명으로 확인
            if (constraintName != null && 
                (constraintName.toLowerCase().contains("account_id") && 
                 constraintName.toLowerCase().contains("course_id"))) {
                return true;
            }
        }

        // SQLException 메시지 확인 (fallback)
        String message = rootCause.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            
            // MySQL: Duplicate entry ... for key 'account_id'
            // PostgreSQL: duplicate key value violates unique constraint
            // H2: Unique index or primary key violation
            boolean isDuplicate = lowerMessage.contains("duplicate") || 
                                  lowerMessage.contains("unique");
            boolean hasAccountCourse = lowerMessage.contains("account_id") && 
                                        lowerMessage.contains("course_id");
            
            if (isDuplicate && hasAccountCourse) {
                return true;
            }
        }

        return false;
    }

    /**
     * 찜하기 삭제 (idempotent - 없어도 에러 안 남)
     */
    @Override
    @Transactional
    public void removeFavorite(Long accountId, Long courseId) {
        Optional<CourseFavorite> existing = favoriteRepository.findByAccount_IdAndCourse_Id(accountId, courseId);
        existing.ifPresent(favoriteRepository::delete);
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
                        true
                ))
                .toList();
    }

    /**
     * 찜 여부 확인
     */
    @Override
    public boolean isFavorite(Long accountId, Long courseId) {
        return favoriteRepository.existsByAccount_IdAndCourse_Id(accountId, courseId);
    }
}
