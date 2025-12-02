package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseQna;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaFileDetail;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaUpdateRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;
import com.softwarecampus.backend.exception.course.BadRequestException;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.repository.course.CourseQnaRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQnaServiceImpl implements CourseQnaService {

    private final CourseQnaRepository qnaRepository;
    private final CourseRepository courseRepository;
    private final AccountRepository accountRepository;
    private final CourseQnaAttachmentService attachmentService;

    @Override
    public Page<QnaResponse> getQnaList(Long courseId, String keyword, Pageable pageable) {
        validateCourse(courseId);

        Page<CourseQna> qnaPage;
        if (keyword != null && !keyword.isBlank()) {
            qnaPage = qnaRepository.searchByCourseIdAndKeyword(courseId, keyword, pageable);
        } else {
            qnaPage = qnaRepository.findByCourseId(courseId, pageable);
        }

        return qnaPage.map(this::toDto);
    }

    @Override
    public QnaResponse getQnaDetail(Long qnaId) {
        CourseQna qna = validateQna(qnaId);
        return toDto(qna);
    }

    @Override
    @Transactional
    public QnaResponse createQuestion(Long courseId, Long writerId, QnaRequest request) {
        Course course = validateCourse(courseId);
        Account writer = accountRepository.findById(writerId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        CourseQna qna = CourseQna.builder()
                .course(course)
                .account(writer)
                .title(request.getTitle())
                .questionText(request.getQuestionText())
                .build();

        CourseQna savedQna = qnaRepository.save(qna);

        // 첨부파일 확정 (임시 저장된 파일을 Q&A에 연결)
        if (request.getFileDetails() != null && !request.getFileDetails().isEmpty()) {
            attachmentService.confirmAttachments(request.getFileDetails(), savedQna.getId());
        }

        return toDto(savedQna);
    }

    @Override
    @Transactional
    public QnaResponse updateQuestion(Long qnaId, Long writerId, QnaUpdateRequest request) {
        CourseQna qna = validateQna(qnaId);
        if (!qna.getAccount().getId().equals(writerId)) {
            throw new ForbiddenException("본인의 질문만 수정할 수 있습니다.");
        }

        // 제목/내용이 제공된 경우에만 업데이트 (null이 아닌 경우)
        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new BadRequestException("제목을 입력해주세요");
            }
            qna.setTitle(request.getTitle());
        }

        if (request.getQuestionText() != null) {
            if (request.getQuestionText().isBlank()) {
                throw new BadRequestException("질문 내용을 입력해주세요");
            }
            qna.setQuestionText(request.getQuestionText());
        }

        // 새로 추가된 파일 확정 (예외 가능성이 있는 작업을 먼저 수행)
        if (request.getFileDetails() != null && !request.getFileDetails().isEmpty()) {
            attachmentService.confirmAttachments(request.getFileDetails(), qnaId);
        }

        // 삭제 요청된 파일 처리 (Soft Delete, 물리적 삭제는 스케줄러에 위임)
        if (request.getDeletedFileIds() != null && !request.getDeletedFileIds().isEmpty()) {
            attachmentService.softDeleteFiles(request.getDeletedFileIds(), qnaId);
        }

        return toDto(qna);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long qnaId, Long writerId) {
        CourseQna qna = validateQna(qnaId);
        if (!qna.getAccount().getId().equals(writerId)) {
            throw new ForbiddenException("본인의 질문만 삭제할 수 있습니다.");
        }

        // 연결된 첨부파일 Soft Delete (물리적 삭제는 스케줄러에 위임)
        attachmentService.softDeleteAllByQnaId(qnaId);

        qna.markDeleted();
    }

    @Override
    @Transactional
    public QnaResponse answerQuestion(Long qnaId, Long adminId, QnaAnswerRequest request) {
        CourseQna qna = validateQna(qnaId);

        if (qna.isAnswered()) {
            throw new ForbiddenException("이미 답변이 완료된 질문입니다.");
        }

        Account admin = accountRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("관리자를 찾을 수 없습니다."));

        qna.writeAnswer(request.getAnswerText(), admin);

        return toDto(qna);
    }

    @Override
    @Transactional
    public QnaResponse updateAnswer(Long qnaId, Long adminId, QnaAnswerRequest request) {
        CourseQna qna = validateQna(qnaId);

        if (request.getAnswerText() == null || request.getAnswerText().trim().isEmpty()) {
            throw new BadRequestException("답변 내용을 입력해야 합니다.");
        }

        if (!qna.isAnswered()) {
            throw new NotFoundException("수정할 답변이 존재하지 않습니다.");
        }

        Account admin = accountRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("관리자를 찾을 수 없습니다."));

        if (qna.getAnsweredBy() != null && !qna.getAnsweredBy().getId().equals(adminId)) {
            throw new ForbiddenException("본인의 답변만 수정할 수 있습니다.");
        }

        qna.writeAnswer(request.getAnswerText(), admin);
        return toDto(qna);
    }

    @Override
    @Transactional
    public void deleteAnswer(Long qnaId, Long adminId) {
        CourseQna qna = validateQna(qnaId);

        if (!qna.isAnswered()) {
            throw new NotFoundException("삭제할 답변이 존재하지 않습니다.");
        }

        if (qna.getAnsweredBy() != null && !qna.getAnsweredBy().getId().equals(adminId)) {
            throw new ForbiddenException("본인의 답변만 삭제할 수 있습니다.");
        }
        qna.setAnswerText(null);
        qna.setAnsweredBy(null);
        qna.setAnswered(false);
    }

    @Override
    public void validateCourseExists(Long courseId) {
        validateCourse(courseId);
    }

    private Course validateCourse(Long courseId) {
        Objects.requireNonNull(courseId, "Course ID must not be null");
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다."));
        if (course.getIsDeleted()) {
            throw new NotFoundException("과정을 찾을 수 없습니다.");
        }
        return course;
    }

    private CourseQna validateQna(Long qnaId) {
        Objects.requireNonNull(qnaId, "QnA ID must not be null");
        CourseQna qna = qnaRepository.findWithDetailsById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A를 찾을 수 없습니다."));
        if (qna.getIsDeleted()) {
            throw new NotFoundException("Q&A를 찾을 수 없습니다.");
        }
        return qna;
    }

    private QnaResponse toDto(CourseQna qna) {
        // 첨부파일 목록 조회
        List<QnaFileDetail> files = attachmentService.getFilesByQnaId(qna.getId());

        return new QnaResponse(
                qna.getId(),
                qna.getTitle(),
                qna.getQuestionText(),
                qna.getAnswerText(),
                qna.getAccount().getId(),
                qna.getAccount().getUserName(),
                qna.getAnsweredBy() != null ? qna.getAnsweredBy().getId() : null,
                qna.getAnsweredBy() != null ? qna.getAnsweredBy().getUserName() : null,
                qna.isAnswered(),
                qna.getCreatedAt(),
                qna.getUpdatedAt(),
                files);
    }
}