package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 과정 리뷰 엔티티
 */
@Entity
@Table(name = "course_review")
public class CourseReview extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private String title;

    private Integer section1Point;
    @Column(columnDefinition = "TEXT")
    private String section1Text;

    private Integer section2Point;
    @Column(columnDefinition = "TEXT")
    private String section2Text;

    private Integer section3Point;
    @Column(columnDefinition = "TEXT")
    private String section3Text;

    private Integer section4Point;
    @Column(columnDefinition = "TEXT")
    private String section4Text;

    private Integer section5Point;
    @Column(columnDefinition = "TEXT")
    private String section5Text;

    private Integer section6Point;
    @Column(columnDefinition = "TEXT")
    private String section6Text;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus courseReviewApproved;

    private LocalDateTime approvedAt;

    protected CourseReview() {
    }

    // 연관관계 편의 메소드
    public void assignCourse(Course course) {
        this.course = course;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getTitle() {
        return title;
    }

    public Integer getSection1Point() {
        return section1Point;
    }

    public String getSection1Text() {
        return section1Text;
    }

    public Integer getSection2Point() {
        return section2Point;
    }

    public String getSection2Text() {
        return section2Text;
    }

    public Integer getSection3Point() {
        return section3Point;
    }

    public String getSection3Text() {
        return section3Text;
    }

    public Integer getSection4Point() {
        return section4Point;
    }

    public String getSection4Text() {
        return section4Text;
    }

    public Integer getSection5Point() {
        return section5Point;
    }

    public String getSection5Text() {
        return section5Text;
    }

    public Integer getSection6Point() {
        return section6Point;
    }

    public String getSection6Text() {
        return section6Text;
    }

    public ApprovalStatus getCourseReviewApproved() {
        return courseReviewApproved;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
}
