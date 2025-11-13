package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;

/**
 * 과정 Q&A 엔티티
 * - 질문과 답변을 하나의 테이블로 관리
 * - 승인 절차 없음
 * - 댓글/대댓글 없음
 */
@Entity
@Table(name = "course_qna")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseQna extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Q&A 제목 */
    private String title;

    /** 질문 내용 */
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** 답변 내용 (강사 또는 관리자 작성) */
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    /** 어떤 과정에 대한 Q&A인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** 질문 작성자 (일반 사용자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private Account writer;

    /** 답변 작성자 (관리자 또는 강사) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by_id")
    private Account answeredBy;

    /** 답변 완료 여부 */
    @Column(name = "is_answered", nullable = false)
    private boolean isAnswered = false;

    /** 답변 등록 메서드 */
    public void writeAnswer(String answerText, Account answeredBy) {
        this.answerText = answerText;
        this.answeredBy = answeredBy;
        this.isAnswered = true;
    }
}
