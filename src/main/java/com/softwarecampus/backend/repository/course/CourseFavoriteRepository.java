package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseFavoriteRepository extends JpaRepository<CourseFavorite, Long> {

    /** 특정 회원이 특정 과정을 찜한 기록을 조회 */
    Optional<CourseFavorite> findByAccount_IdAndCourse_Id(Long accountId, Long courseId);

    /** 특정 회원이 찜한 모든 과정 목록 조회 */
    List<CourseFavorite> findByAccount_Id(Long accountId);

    /** 특정 회원이 특정 과정을 찜했는지 여부 확인 */
    boolean existsByAccount_IdAndCourse_Id(Long accountId, Long courseId);

    /** 특정 회원이 찜한 과정 수 (마이페이지 통계용) */
    Long countByAccount_Id(Long accountId);
}
