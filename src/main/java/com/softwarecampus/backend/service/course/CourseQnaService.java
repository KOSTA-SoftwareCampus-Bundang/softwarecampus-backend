package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;

import java.util.List;

public interface CourseQnaService {

    List<QnaResponse> getQnaList(Long courseId);

    QnaResponse getQnaDetail(Long qnaId);

    QnaResponse createQuestion(Long courseId, Long writerId, QnaRequest request);

    QnaResponse updateQuestion(Long qnaId, Long writerId, QnaRequest request);

    void deleteQuestion(Long qnaId, Long writerId);

    QnaResponse answerQuestion(Long qnaId, Long adminId, QnaAnswerRequest request);

    QnaResponse updateAnswer(Long qnaId, Long adminId, QnaAnswerRequest request);

    void deleteAnswer(Long qnaId, Long adminId);
}