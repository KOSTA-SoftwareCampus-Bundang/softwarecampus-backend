package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseQnaService {

    Page<QnaResponse> getQnaList(Long courseId, String keyword, Pageable pageable);

    QnaResponse getQnaDetail(Long qnaId);

    QnaResponse createQuestion(Long courseId, Long writerId, QnaRequest request);

    QnaResponse updateQuestion(Long qnaId, Long writerId, QnaRequest request);

    void deleteQuestion(Long qnaId, Long writerId);

    QnaResponse answerQuestion(Long qnaId, Long adminId, QnaAnswerRequest request);

    QnaResponse updateAnswer(Long qnaId, Long adminId, QnaAnswerRequest request);

    void deleteAnswer(Long qnaId, Long adminId);
}