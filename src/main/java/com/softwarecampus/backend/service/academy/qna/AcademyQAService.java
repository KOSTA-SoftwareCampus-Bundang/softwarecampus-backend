package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;

import java.util.List;

public interface AcademyQAService {

    /**
     * Q/A 조회(리스트 형식)
     */
    List<QAResponse> getQAsByAcademyId(Long academyId);

    /**
     * Q/A 상세보기
     */
    QAResponse getAcademyQADetail(Long qaId, Long academyId);

    /**
     * 질문 등록
     */
    QAResponse createQuestion(Long academyId, QACreateRequest request, Long userId);

    /**
     * 질문 수정
     */
    QAResponse updateQuestion(Long academyId, Long qaId, QAUpdateRequest request, Long userId);

    /**
     * Q/A (질문과 답변 전체) 삭제
     */
    void deleteQuestion(Long qaId, Long academyId, Long userId);

    /**
     * 답변 등록 (신규)
     */
    QAResponse answerQuestion(Long academyId, Long qaId, QAUpdateRequest request, Long userId);

    /**
     * 답변 수정 (기존 답변만)
     */
    QAResponse updateAnswer(Long academyId, Long qaId, QAUpdateRequest request, Long userId);

    /**
     * 답변 삭제
     */
    QAResponse deleteAnswer(Long qaId, Long academyId, Long userId);
}
