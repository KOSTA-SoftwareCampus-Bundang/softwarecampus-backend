package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 과정 질문 엔티티
 */
@Entity
@Table(name = "course_question")
public class CourseQuestion extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private String questionNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    @OneToMany(mappedBy = "courseQuestion")
    private List<CourseAnswer> answers = new ArrayList<>();

    protected CourseQuestion() {
    }

    // 연관관계 편의 메소드
    public void assignCourse(Course course) {
        this.course = course;
    }

    public void addAnswer(CourseAnswer answer) {
        answers.add(answer);
        answer.assignQuestion(this);
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getQuestionNumber() {
        return questionNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public List<CourseAnswer> getAnswers() {
        return answers;
    }
}
