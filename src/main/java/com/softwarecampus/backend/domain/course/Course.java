package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 개설 기관 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    /** 과정 카테고리 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CourseCategory category;

    /** 과정 기본 정보 */
    @Column(nullable = false)
    private String name;

    private LocalDate recruitStart;
    private LocalDate recruitEnd;
    private LocalDate courseStart;
    private LocalDate courseEnd;

    private Integer cost;
    private String classDay;
    private String location;

    private boolean isKdt;
    private boolean isNailbaeum;
    private boolean isOffline = true;

    @Column(columnDefinition = "TEXT")
    private String requirement;

    /** 승인 관련 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus isApproved = ApprovalStatus.PENDING;

    private LocalDateTime approvedAt;

    /** 커리큘럼 */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourseCurriculum> curriculums = new ArrayList<>();

    /** 리뷰 */
    @OneToMany(mappedBy = "course", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<CourseReview> reviews = new ArrayList<>();

    /** Q&A */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourseQna> qnaList = new ArrayList<>();

    /** 즐겨찾기 */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourseFavorite> favorites = new ArrayList<>();

    // ======================
    // 연관관계 편의 메서드
    // ======================

    // Curriculum
    public void addCurriculum(CourseCurriculum curriculum) {
        curriculums.add(curriculum);
        curriculum.setCourse(this);
    }

    public void removeCurriculum(CourseCurriculum curriculum) {
        curriculums.remove(curriculum);
        curriculum.setCourse(null);
    }

    // Review
    public void addReview(CourseReview review) {
        reviews.add(review);
        review.setCourse(this);
    }

    public void removeReview(CourseReview review) {
        reviews.remove(review);
        review.setCourse(null);
    }

    // QnA
    public void addQna(CourseQna qna) {
        qnaList.add(qna);
        qna.setCourse(this);
    }

    public void removeQna(CourseQna qna) {
        qnaList.remove(qna);
        qna.setCourse(null);
    }

    // Favorite
    public void addFavorite(CourseFavorite favorite) {
        favorites.add(favorite);
        favorite.setCourse(this);
    }

    public void removeFavorite(CourseFavorite favorite) {
        favorites.remove(favorite);
        favorite.setCourse(null);
    }

    // 승인 처리 로직 (관리자용)
    public void approve() {
        this.isApproved = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject() {
        this.isApproved = ApprovalStatus.REJECTED;
        this.approvedAt = null;
    }

    public CategoryType getCategoryType() {
        return this.category.getCategoryType();
    }
}
