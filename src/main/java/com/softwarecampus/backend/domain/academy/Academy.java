package com.softwarecampus.backend.domain.academy;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import com.softwarecampus.backend.domain.course.Course;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 훈련기관 엔티티
 */
@Entity
@Table(name = "academy",
    indexes = {
        @Index(name = "idx_academy_approved", columnList = "isApproved,isDeleted"),
        @Index(name = "idx_academy_business_number", columnList = "businessNumber")
    }
)
public class Academy extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(name = "business_number")
    private String businessNumber;

    private String phone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus isApproved;

    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "academy")
    private List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "academy")
    private List<AcademyQuestion> questions = new ArrayList<>();

    protected Academy() {
    }

    // 연관관계 편의 메소드
    public void addCourse(Course course) {
        courses.add(course);
        course.assignAcademy(this);
    }

    public void addQuestion(AcademyQuestion question) {
        questions.add(question);
        question.assignAcademy(this);
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getBusinessNumber() {
        return businessNumber;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public ApprovalStatus getIsApproved() {
        return isApproved;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public List<AcademyQuestion> getQuestions() {
        return questions;
    }
}
