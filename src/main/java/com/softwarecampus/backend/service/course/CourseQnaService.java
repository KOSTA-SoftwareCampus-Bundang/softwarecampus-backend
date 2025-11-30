package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseQnaService {

    Page<QnaResponse> getQnaList(CategoryType type, Long courseId, String keyword, Pageable pageable);

    QnaResponse getQnaDetail(CategoryType type, Long qnaId);

    QnaResponse createQuestion(CategoryType type, Long courseId, Long writerId, QnaRequest request);

    QnaResponse updateQuestion(CategoryType type, Long qnaId, Long writerId, QnaRequest request);

    void deleteQuestion(CategoryType type, Long qnaId, Long writerId);

    QnaResponse answerQuestion(CategoryType type, Long qnaId, Long adminId, QnaAnswerRequest request);

    QnaResponse updateAnswer(CategoryType type, Long qnaId, Long adminId, QnaAnswerRequest request);

    void deleteAnswer(CategoryType type, Long qnaId, Long adminId);
}