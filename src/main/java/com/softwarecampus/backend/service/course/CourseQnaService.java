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

    void deleteQuestion(Long qnaId, Long writerId);

    QnaResponse answerQuestion(Long qnaId, Long adminId, QnaAnswerRequest request);

    QnaResponse updateAnswer(Long qnaId, Long adminId, QnaAnswerRequest request);

    void deleteAnswer(Long qnaId, Long adminId);

    /**
     * 과정 존재 여부 검증 (파일 업로드 등에서 사용)
     * 
     * @param courseId 검증할 과정 ID
     * @throws com.softwarecampus.backend.exception.course.NotFoundException 과정을 찾을
     *                                                                       수 없는 경우
     */
    void validateCourseExists(Long courseId);
}