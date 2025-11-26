package com.softwarecampus.backend.service.home;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.dto.home.HomeCourseDTO;
import com.softwarecampus.backend.dto.home.HomeResponseDTO;
import com.softwarecampus.backend.repository.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 메인페이지 전용 서비스 구현
 * 기존 CourseServiceImpl과 독립적으로 동작
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeServiceImpl implements HomeService {

        private final CourseRepository courseRepository;

        /**
         * 메인페이지 데이터 조회
         * 재직자/취업예정자 베스트, 마감 임박 과정을 한번에 조회
         */
        @Override
        public HomeResponseDTO getHomePageData() {
                return HomeResponseDTO.builder()
                                .employeeBest(getEmployeeBestCourses(4))
                                .jobSeekerBest(getJobSeekerBestCourses(4))
                                .closingSoon(getClosingSoonCourses(4))
                                .build();
        }

        /**
         * 재직자 베스트 과정 조회
         * - 승인된 과정만 (APPROVED)
         * - 삭제되지 않은 과정 (deletedAt is null)
         * - 정렬: (찜 + 리뷰 수) DESC, 리뷰 수 DESC, ID ASC
         */
        private List<HomeCourseDTO> getEmployeeBestCourses(int limit) {
                return courseRepository.findByCategory_CategoryTypeAndDeletedAtIsNull(CategoryType.EMPLOYEE)
                                .stream()
                                .filter(course -> course.getIsApproved() == ApprovalStatus.APPROVED)
                                .sorted(Comparator
                                                .comparingInt((Course c) -> c.getFavorites().size()
                                                                + c.getReviews().size())
                                                .reversed()
                                                .thenComparing(Comparator
                                                                .comparingInt((Course c) -> c.getReviews().size())
                                                                .reversed())
                                                .thenComparing(Course::getId))
                                .limit(limit)
                                .map(HomeCourseDTO::fromEntity)
                                .toList();
        }

        /**
         * 취업예정자 베스트 과정 조회
         * - 승인된 과정만 (APPROVED)
         * - 삭제되지 않은 과정 (deletedAt is null)
         * - 정렬: (찜 + 리뷰 수) DESC, 리뷰 수 DESC, ID ASC
         */
        private List<HomeCourseDTO> getJobSeekerBestCourses(int limit) {
                return courseRepository.findByCategory_CategoryTypeAndDeletedAtIsNull(CategoryType.JOB_SEEKER)
                                .stream()
                                .filter(course -> course.getIsApproved() == ApprovalStatus.APPROVED)
                                .sorted(Comparator
                                                .comparingInt((Course c) -> c.getFavorites().size()
                                                                + c.getReviews().size())
                                                .reversed()
                                                .thenComparing(Comparator
                                                                .comparingInt((Course c) -> c.getReviews().size())
                                                                .reversed())
                                                .thenComparing(Course::getId))
                                .limit(limit)
                                .map(HomeCourseDTO::fromEntity)
                                .toList();
        }

        /**
         * 마감 임박 과정 조회
         * - 승인된 과정만 (APPROVED)
         * - 삭제되지 않은 과정 (deletedAt is null)
         * - 모집 종료일이 오늘 ~ 7일 후 이내
         * - 모집 종료일 가까운 순으로 정렬
         */
        private List<HomeCourseDTO> getClosingSoonCourses(int limit) {
                LocalDate today = LocalDate.now();
                LocalDate endDate = today.plusDays(7);

                // 재직자 + 취업예정자 모두 조회
                List<Course> employeeCourses = courseRepository
                                .findByCategory_CategoryTypeAndDeletedAtIsNull(CategoryType.EMPLOYEE);
                List<Course> jobSeekerCourses = courseRepository
                                .findByCategory_CategoryTypeAndDeletedAtIsNull(CategoryType.JOB_SEEKER);

                return Stream.concat(employeeCourses.stream(), jobSeekerCourses.stream())
                                .filter(course -> course.getIsApproved() == ApprovalStatus.APPROVED)
                                .filter(course -> course.getRecruitEnd() != null
                                                && !course.getRecruitEnd().isBefore(today)
                                                && !course.getRecruitEnd().isAfter(endDate))
                                .sorted(Comparator.comparing(Course::getRecruitEnd))
                                .limit(limit)
                                .map(HomeCourseDTO::fromEntity)
                                .toList();
        }
}
