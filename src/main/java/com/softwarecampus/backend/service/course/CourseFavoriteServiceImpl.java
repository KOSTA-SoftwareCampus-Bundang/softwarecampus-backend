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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseFavoriteServiceImpl implements CourseFavoriteService {

    private final CourseFavoriteRepository favoriteRepository;
    private final CourseRepository courseRepository;
    private final AccountRepository accountRepository;

    /**
     * 찜하기 토글
     * 이미 찜했으면 삭제, 아니면 등록
     */
    @Override
    @Transactional
    public CourseFavoriteResponseDTO toggleFavorite(String type, Long accountId, Long courseId) {

        System.out.println("toggleFavorite called with type = " + type);

        Optional<CourseFavorite> existing = favoriteRepository.findByAccount_IdAndCourse_Id(accountId, courseId);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return new CourseFavoriteResponseDTO(courseId, false); // false = 찜 해제됨
        } else {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다."));
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 사용자가 존재하지 않습니다."));

            CourseFavorite favorite = CourseFavorite.builder()
                    .account(account)
                    .course(course)
                    .build();
            favoriteRepository.save(favorite);

            return new CourseFavoriteResponseDTO(courseId, true); // true = 찜 등록됨
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
