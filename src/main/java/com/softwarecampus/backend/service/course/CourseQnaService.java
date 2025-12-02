package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaUpdateRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseQnaService {

    Page<QnaResponse> getQnaList(Long courseId, String keyword, Pageable pageable);

    QnaResponse getQnaDetail(Long qnaId);

    QnaResponse createQuestion(Long courseId, Long writerId, QnaRequest request);

    QnaResponse updateQuestion(Long qnaId, Long writerId, QnaUpdateRequest request);

    /** 질문 삭제 (질문자 본인 또는 관리자만 가능) */
    void deleteQuestion(Long qnaId, Long userId);

    /** 답변 등록 (관리자 또는 해당 과정 기관 담당자) */
    QnaResponse answerQuestion(Long qnaId, Long responderId, QnaAnswerRequest request);

    /** 답변 수정 (관리자 또는 본인이 작성한 답변만) */
    QnaResponse updateAnswer(Long qnaId, Long responderId, QnaAnswerRequest request);

    /** 답변 삭제 (관리자 또는 본인이 작성한 답변만) */
    void deleteAnswer(Long qnaId, Long responderId);

    /**
     * 과정 존재 여부 검증 (파일 업로드 등에서 사용)
     * 
     * @param courseId 검증할 과정 ID
     * @throws com.softwarecampus.backend.exception.course.NotFoundException 과정을 찾을
     *                                                                       수 없는 경우
     */
    void validateCourseExists(Long courseId);
}