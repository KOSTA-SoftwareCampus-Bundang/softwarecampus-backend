package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 과정 엔티티
 */
@Entity
@Table(name = "course",
    indexes = {
        @Index(name = "idx_course_approved", columnList = "isApproved,isDeleted"),
        @Index(name = "idx_course_academy", columnList = "academy_id"),
        @Index(name = "idx_course_category", columnList = "course_category_id"),
        @Index(name = "idx_course_recruit_dates", columnList = "recrutStart,recrutEnd")
    }
)
public class Course extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_category_id", nullable = false)
    private CourseCategory courseCategory;

    @Column(nullable = false)
    private String name;

    @Column(name = "recrut_start")
    private LocalDateTime recrutStart;

    @Column(name = "recrut_end")
    private LocalDateTime recrutEnd;

    @Column(name = "course_start")
    private LocalDateTime courseStart;

    @Column(name = "course_end")
    private LocalDateTime courseEnd;

    private String cost;

    @Column(name = "class_day")
    private String classDay;

    private String location;

    @Column(name = "is_nailbaeum")
    private boolean isNailbaeum;

    @Column(name = "is_kdt")
    private boolean isKdt;

    private String requirement;

    @Column(name = "is_offline")
    private boolean isOffline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus isApproved;

    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "course")
    private List<CourseReview> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "course")
    private List<CourseQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "course")
    private List<CourseCurriculum> curriculums = new ArrayList<>();

    protected Course() {
    }

    // 연관관계 편의 메소드
    public void assignAcademy(Academy academy) {
        this.academy = academy;
    }

    public void assignCategory(CourseCategory category) {
        this.courseCategory = category;
    }

    public void addReview(CourseReview review) {
        reviews.add(review);
        review.assignCourse(this);
    }

    public void addQuestion(CourseQuestion question) {
        questions.add(question);
        question.assignCourse(this);
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Academy getAcademy() {
        return academy;
    }

    public CourseCategory getCourseCategory() {
        return courseCategory;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getRecrutStart() {
        return recrutStart;
    }

    public LocalDateTime getRecrutEnd() {
        return recrutEnd;
    }

    public LocalDateTime getCourseStart() {
        return courseStart;
    }

    public LocalDateTime getCourseEnd() {
        return courseEnd;
    }

    public String getCost() {
        return cost;
    }

    public String getClassDay() {
        return classDay;
    }

    public String getLocation() {
        return location;
    }

    public boolean isNailbaeum() {
        return isNailbaeum;
    }

    public boolean isKdt() {
        return isKdt;
    }

    public String getRequirement() {
        return requirement;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public ApprovalStatus getIsApproved() {
        return isApproved;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public List<CourseReview> getReviews() {
        return reviews;
    }

    public List<CourseQuestion> getQuestions() {
        return questions;
    }

    public List<CourseCurriculum> getCurriculums() {
        return curriculums;
    }
}
