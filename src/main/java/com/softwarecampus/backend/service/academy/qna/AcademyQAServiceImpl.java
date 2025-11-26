package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.exception.academy.AcademyErrorCode;
import com.softwarecampus.backend.exception.academy.AcademyException;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.academy.academyQA.AcademyQARepository;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademyQAServiceImpl implements AcademyQAService {

    private final AcademyQARepository academyQARepository;
    private final AcademyRepository academyRepository;
    private final AttachmentRepository attachmentRepository;

    private AcademyQA findQAAndValidateAcademy(Long qaId, Long academyId) {
        AcademyQA qa = academyQARepository.findById(qaId)
                .orElseThrow(() -> new AcademyException(AcademyErrorCode.ACADEMY_NOT_FOUND));

        if (qa.getAcademy() == null) {
            throw new AcademyException(AcademyErrorCode.QA_MISSING_ACADEMY_RELATION);
        }

        if (!qa.getAcademy().getId().equals(academyId)) {
            throw new AcademyException(AcademyErrorCode.QA_MISMATCH_ACADEMY);
        }
        return qa;
    }

    /**
     * 훈련기관 Q/A 조회(리스트 형식)
     */
    @Override
    public List<QAResponse> getQAsByAcademyId(Long academyId) {
        return academyQARepository.findAllByAcademyId(academyId).stream()
                .map(QAResponse::from)
                .collect(Collectors.toList());
    }

    /**
     *  훈련기관 Q/A 상세보기
     */
    @Override
    public QAResponse getAcademyQADetail(Long qaId, Long academyId) {
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);
        return QAResponse.from(qa);
    }

    /**
     *  훈련기관 Q/A 질문 등록
     */
    @Override
    @Transactional
    public QAResponse createQuestion(Long academyId, QACreateRequest request) {
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new AcademyException(AcademyErrorCode.ACADEMY_NOT_FOUND));

        AcademyQA qa = AcademyQA.builder()
                .title(request.getTitle())
                .questionText(request.getQuestionText())
                .academy(academy)
                .build();

        return QAResponse.from(academyQARepository.save(qa));
    }

    /**
     *  훈련기관 Q/A 질문 수정
     */
    @Override
    @Transactional
    public QAResponse updateQuestion(Long academyId, Long qaId, QAUpdateRequest request) {
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);
        qa.updateQuestion(request.getTitle(), request.getQuestionText());

        return QAResponse.from(qa);
    }

    /**
     *  훈련기관 Q/A (질문,답변 전체) 삭제
     */
    @Override
    @Transactional
    public void deleteQuestion(Long qaId, Long academyId) {
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);

        // 연결된 첨푸파일 삭제
        attachmentRepository.softDeleteAllByCategoryTypeAndCategoryId(
                AttachmentCategoryType.QNA,
                qaId
        );

        academyQARepository.delete(qa);
    }

    /**
     *  답변 등록 / 수정
     */
    @Override
    @Transactional
    public QAResponse updateAnswer(Long academyId, Long qaId,  QAUpdateRequest request) {
        if (request.getAnswerText() == null) {
            throw new AcademyException(AcademyErrorCode.ANSWER_TEXT_REQUIRED);
        }

        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);
        qa.updateAnswer(request.getAnswerText());

        return QAResponse.from(qa);
    }

    /**
     *  답변 삭제
     */
    @Override
    @Transactional
    public QAResponse deleteAnswer(Long qaId, Long academyId) {
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);

        if (qa.getAnswerText() == null) {
            throw new AcademyException(AcademyErrorCode.ANSWER_NOT_EXIST);
        }

        qa.deleteAnswer();
        return QAResponse.from(qa);
    }
}
