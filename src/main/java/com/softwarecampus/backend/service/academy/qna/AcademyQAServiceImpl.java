package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.exception.academy.AcademyErrorCode;
import com.softwarecampus.backend.exception.academy.AcademyException;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.academy.academyQA.AcademyQARepository;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademyQAServiceImpl implements AcademyQAService {

    private final AcademyQARepository academyQARepository;
    private final AcademyRepository academyRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final AccountRepository accountRepository;

    private AcademyQA findQAAndValidateAcademy(Long qaId, Long academyId) {
        Objects.requireNonNull(qaId, "QA ID must not be null");
        Objects.requireNonNull(academyId, "Academy ID must not be null");
        AcademyQA qa = academyQARepository.findById(qaId)
                .orElseThrow(() -> new AcademyException(AcademyErrorCode.QA_NOT_FOUND));

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
     * 훈련기관 Q/A 상세보기
     */
    @Override
    public QAResponse getAcademyQADetail(Long qaId, Long academyId) {
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);
        return QAResponse.from(qa);
    }

    /**
     * 훈련기관 Q/A 질문 등록
     */
    @Override
    @Transactional
    public QAResponse createQuestion(Long academyId, QACreateRequest request, Long userId) {
        Objects.requireNonNull(academyId, "Academy ID must not be null");
        Objects.requireNonNull(userId, "User ID must not be null");
        Academy academy = academyRepository.findById(academyId)
                .orElseThrow(() -> new AcademyException(AcademyErrorCode.ACADEMY_NOT_FOUND));

        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new AcademyException(AcademyErrorCode.USER_NOT_FOUND));

        AcademyQA qa = AcademyQA.builder()
                .title(request.getTitle())
                .questionText(request.getQuestionText())
                .academy(academy)
                .account(account)
                .build();

        AcademyQA save = academyQARepository.save(qa);

        // 첨부파일 확정 로직 : 파일 상세 정보가 있다면 확정
        if (request.getFileDetails() != null && !request.getFileDetails().isEmpty()) {
            attachmentService.confirmAttachments(
                    request.getFileDetails(),
                    save.getId(),
                    AttachmentCategoryType.QNA);
        }
        return QAResponse.from(save);
    }

    /**
     * 답변 등록 (신규)
     */
    @Override
    @Transactional
    public QAResponse answerQuestion(Long academyId, Long qaId, QAUpdateRequest request, Long userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);

        if (request.getAnswerText() == null || request.getAnswerText().trim().isEmpty()) {
            throw new AcademyException(AcademyErrorCode.ANSWER_TEXT_REQUIRED);
        }

        // 이미 답변이 있으면 에러
        if (qa.getAnswerText() != null) {
            throw new AcademyException(AcademyErrorCode.ANSWER_ALREADY_EXISTS);
        }

        Account answeredBy = accountRepository.findById(userId)
                .orElseThrow(() -> new AcademyException(AcademyErrorCode.USER_NOT_FOUND));

        qa.updateAnswer(request.getAnswerText(), answeredBy);
        return QAResponse.from(qa);
    }

    /**
     * 답변 수정 (기존 답변만)
     */
    @Override
    @Transactional
    public QAResponse updateAnswer(Long academyId, Long qaId, QAUpdateRequest request, Long userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);

        if (request.getAnswerText() == null || request.getAnswerText().trim().isEmpty()) {
            throw new AcademyException(AcademyErrorCode.ANSWER_TEXT_REQUIRED);
        }

        // 답변이 없으면 에러 (수정만 가능)
        if (qa.getAnswerText() == null) {
            throw new AcademyException(AcademyErrorCode.ANSWER_NOT_EXIST);
        }

        // 작성자 검증
        if (qa.getAnsweredBy() != null && !qa.getAnsweredBy().getId().equals(userId)) {
            throw new AcademyException(AcademyErrorCode.ANSWER_NOT_OWNER);
        }

        Account answeredBy = accountRepository.findById(userId)
                .orElseThrow(() -> new AcademyException(AcademyErrorCode.USER_NOT_FOUND));

        qa.updateAnswer(request.getAnswerText(), answeredBy);
        return QAResponse.from(qa);
    }

    /**
     * 훈련기관 Q/A 질문 수정
     */
    @Override
    @Transactional
    public QAResponse updateQuestion(Long academyId, Long qaId, QAUpdateRequest request, Long userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);

        // 작성자 검증
        if (!qa.getAccount().getId().equals(userId)) {
            throw new ForbiddenException("본인의 질문만 수정할 수 있습니다.");
        }

        qa.updateQuestion(request.getTitle(), request.getQuestionText());

        // 삭제 파일 처리
        if (request.getDeletedFileIds() != null && !request.getDeletedFileIds().isEmpty()) {
            List<Attachment> attachmentsToProcess = attachmentRepository.findAllById(request.getDeletedFileIds());

            // 요청된 첨부파일이 현재 Q&A에 속하는지 검증
            for (Attachment attachment : attachmentsToProcess) {
                if (!AttachmentCategoryType.QNA.equals(attachment.getCategoryType())
                        || !qaId.equals(attachment.getCategoryId())) {
                    throw new AcademyException(AcademyErrorCode.ATTACHMENT_NOT_BELONG_TO_QA);
                }
            }

            attachmentsToProcess.forEach(Attachment::softDelete);
            attachmentService.hardDeleteS3Files(attachmentsToProcess);
        }

        // 새로운 파일 확정 처리
        if (request.getNewFileDetails() != null && !request.getNewFileDetails().isEmpty()) {
            attachmentService.confirmAttachments(
                    request.getNewFileDetails(),
                    qaId,
                    AttachmentCategoryType.QNA);
        }
        return QAResponse.from(qa);
    }

    /**
     * 훈련기관 Q/A (질문,답변 전체) 삭제
     */
    @Override
    @Transactional
    public void deleteQuestion(Long qaId, Long academyId, Long userId) {
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);

        if (!qa.getAccount().getId().equals(userId)) {
            throw new ForbiddenException("본인의 질문만 삭제할 수 있습니다.");
        }

        // 연결된 첨부파일 삭제
        List<Attachment> attachmentsToHardDelete = attachmentService.softDeleteAllByCategoryAndId(
                AttachmentCategoryType.QNA,
                qaId);
        if (attachmentsToHardDelete != null) {
            attachmentService.hardDeleteS3Files(attachmentsToHardDelete);
        }
        academyQARepository.delete(qa);
    }

    /**
     * 훈련기관 답변 삭제
     */
    @Override
    @Transactional
    public QAResponse deleteAnswer(Long qaId, Long academyId, Long userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        AcademyQA qa = findQAAndValidateAcademy(qaId, academyId);

        if (qa.getAnswerText() == null) {
            throw new AcademyException(AcademyErrorCode.ANSWER_NOT_EXIST);
        }

        // 작성자 검증
        if (qa.getAnsweredBy() != null && !qa.getAnsweredBy().getId().equals(userId)) {
            throw new AcademyException(AcademyErrorCode.ANSWER_NOT_OWNER);
        }

        qa.deleteAnswer();
        return QAResponse.from(qa);
    }
}
