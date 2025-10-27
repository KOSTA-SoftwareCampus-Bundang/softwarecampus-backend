package com.softwarecampus.backend.domain.academy;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 기관 질문 엔티티
 */
@Entity
@Table(name = "academy_question")
public class AcademyQuestion extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    private String questionNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    @OneToMany(mappedBy = "academyQuestion")
    private List<AcademyAnswer> answers = new ArrayList<>();

    protected AcademyQuestion() {
    }

    // 연관관계 편의 메소드
    public void assignAcademy(Academy academy) {
        this.academy = academy;
    }

    public void addAnswer(AcademyAnswer answer) {
        answers.add(answer);
        answer.assignQuestion(this);
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Academy getAcademy() {
        return academy;
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

    public List<AcademyAnswer> getAnswers() {
        return answers;
    }
}
