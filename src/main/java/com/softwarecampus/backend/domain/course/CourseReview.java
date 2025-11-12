package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "review", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<ReviewSection> sections = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus reviewApproved = ApprovalStatus.PENDING;

    private LocalDateTime approvedAt;

    // ✅ 헬퍼 메서드
    public void addSection(ReviewSection section) {
        sections.add(section);
        section.setReview(this);
    }

    public void removeSection(ReviewSection section) {
        sections.remove(section);
        section.setReview(null);
    }

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourseReviewRecommend> recommends = new ArrayList<>();

    public void addRecommend(CourseReviewRecommend recommend) {
        recommends.add(recommend);
        recommend.setReview(this);
    }

    public void removeRecommend(CourseReviewRecommend recommend) {
        recommends.remove(recommend);
        recommend.setReview(null);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

}
