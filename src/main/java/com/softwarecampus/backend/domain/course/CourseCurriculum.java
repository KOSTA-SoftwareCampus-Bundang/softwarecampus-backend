package com.softwarecampus.backend.domain.course;

import jakarta.persistence.*;

/**
 * 과정 커리큘럼 엔티티
 */
@Entity
@Table(name = "course_curriculum")
public class CourseCurriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "chapter_name")
    private String chapterName;

    @Column(name = "chapter_detail", columnDefinition = "TEXT")
    private String chapterDetail;

    @Column(name = "chapter_time")
    private Integer chapterTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_tag_id")
    private CourseTag courseTag;

    protected CourseCurriculum() {
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

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public String getChapterName() {
        return chapterName;
    }

    public String getChapterDetail() {
        return chapterDetail;
    }

    public Integer getChapterTime() {
        return chapterTime;
    }

    public CourseTag getCourseTag() {
        return courseTag;
    }
}
