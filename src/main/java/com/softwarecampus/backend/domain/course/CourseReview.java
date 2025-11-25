package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReview extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // 기타 의견 (자유 텍스트)
    @Column(columnDefinition = "TEXT")
    private String comment;

    // 리뷰 승인 상태 (PENDING / APPROVED / REJECTED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus;

    // child 관계
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewSection> sections = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseReviewAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewLike> likes = new ArrayList<>();

    /**
     * 평균 점수 계산 (Section 평균)
     * 프론트 or 서비스 단에서도 자주 사용할 가능성이 높아 여기에 도메인 로직 포함
     */
    public double calculateAverageScore() {
        if (sections == null || sections.isEmpty()) return 0.0;

        return sections.stream()
                .mapToInt(ReviewSection::getScore)
                .average()
                .orElse(0.0);
    }
}

