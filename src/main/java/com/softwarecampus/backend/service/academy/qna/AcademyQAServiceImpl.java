package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.academy.academyQA.AcademyQARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademyQAServiceImpl implements AcademyQAService {

    private final AcademyQARepository academyQARepository;
    private final AcademyRepository academyRepository;

    private AcademyQA findQAAndValidateAcademy(Long qaId, Long questionId) {
        AcademyQA qa = academyQARepository.findById(qaId)
                .orElseThrow(() -> new NoSuchElementException("Academy QA Not Found"));

        if (!qa.getAcademy().getId().equals(questionId)) {
            throw new IllegalArgumentException("Question Id and Academy Id do not match");
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
    public QAResponse createQuestion(QACreateRequest request) {
        Academy academy = academyRepository.findById(request.getAcademyId())
                .orElseThrow(() -> new NoSuchElementException("Academy Not Found"));

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

        if (request.getTitle() != null) qa.setTitle(request.getTitle());
        if (request.getQuestionText() != null) qa.setQuestionText(request.getQuestionText());

        return QAResponse.from(qa);
    }

    /**
     *  훈련기관 Q/A (질문,답변 전체) 삭제
     */
    @Override
    @Transactional
    public void deleteQuestion(Long qaId, Long academyId) {
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);
        academyQARepository.delete(qa);
    }

    /**
     *  답변 등록 / 수정
     */
    @Override
    @Transactional
    public QAResponse updateAnswer(Long qaId, Long academyId, QAUpdateRequest request) {
        if (request.getAnswerText() == null) {
            throw new IllegalArgumentException("Answer Text Not Found");
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
            throw new NoSuchElementException("Answer Text Not Found");
        }

        qa.deleteAnswer();
        return QAResponse.from(qa);
    }







}
