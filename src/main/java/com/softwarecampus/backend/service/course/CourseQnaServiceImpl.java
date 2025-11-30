package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseQna;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
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

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQnaServiceImpl implements CourseQnaService {

    private final CourseQnaRepository qnaRepository;
    private final CourseRepository courseRepository;
    private final AccountRepository accountRepository;

    @Override
    public Page<QnaResponse> getQnaList(CategoryType type, Long courseId, String keyword, Pageable pageable) {
        Course course = validateCourse(type, courseId);

        Page<CourseQna> qnaPage;
        if (keyword != null && !keyword.isBlank()) {
            qnaPage = qnaRepository.searchByCourseIdAndKeyword(courseId, keyword, pageable);
        } else {
            qnaPage = qnaRepository.findByCourseId(courseId, pageable);
        }

        return qnaPage.map(this::toDto);
    }

    @Override
    public QnaResponse getQnaDetail(CategoryType type, Long qnaId) {
        CourseQna qna = validateQna(type, qnaId);
        return toDto(qna);
    }

    @Override
    @Transactional
    public QnaResponse createQuestion(CategoryType type, Long courseId, Long writerId, QnaRequest request) {
        Course course = validateCourse(type, courseId);
        Account writer = accountRepository.findById(writerId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        CourseQna qna = CourseQna.builder()
                .course(course)
                .account(writer)
                .title(request.getTitle())
                .questionText(request.getQuestionText())
                .build();

        return toDto(qnaRepository.save(qna));
    }

    @Override
    @Transactional
    public QnaResponse updateQuestion(CategoryType type, Long qnaId, Long writerId, QnaRequest request) {
        CourseQna qna = validateQna(type, qnaId);
        if (!qna.getAccount().getId().equals(writerId)) {
            throw new ForbiddenException("본인의 질문만 수정할 수 있습니다.");
        }

        qna.setTitle(request.getTitle());
        qna.setQuestionText(request.getQuestionText());
        return toDto(qna);
    }

    @Override
    @Transactional
    public void deleteQuestion(CategoryType type, Long qnaId, Long writerId) {
        CourseQna qna = validateQna(type, qnaId);
        if (!qna.getAccount().getId().equals(writerId)) {
            throw new ForbiddenException("본인의 질문만 삭제할 수 있습니다.");
        }
        qna.markDeleted();
    }

    @Override
    @Transactional
    public QnaResponse answerQuestion(CategoryType type, Long qnaId, Long adminId, QnaAnswerRequest request) {
        CourseQna qna = validateQna(type, qnaId);

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
    public QnaResponse updateAnswer(CategoryType type, Long qnaId, Long adminId, QnaAnswerRequest request) {
        CourseQna qna = validateQna(type, qnaId);

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
    public void deleteAnswer(CategoryType type, Long qnaId, Long adminId) {
        CourseQna qna = validateQna(type, qnaId);

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

    private Course validateCourse(CategoryType type, Long courseId) {
        Objects.requireNonNull(courseId, "Course ID must not be null");
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다."));
        if (course.getIsDeleted()) {
            throw new NotFoundException("과정을 찾을 수 없습니다.");
        }
        if (course.getCategoryType() != type) {
            throw new NotFoundException("잘못된 타입의 과정입니다.");
        }
        return course;
    }

    private CourseQna validateQna(CategoryType type, Long qnaId) {
        Objects.requireNonNull(qnaId, "QnA ID must not be null");
        CourseQna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A를 찾을 수 없습니다."));
        if (qna.getIsDeleted()) {
            throw new NotFoundException("Q&A를 찾을 수 없습니다.");
        }
        // Validate if QnA belongs to a course of the correct type
        if (qna.getCourse().getCategoryType() != type) {
            throw new NotFoundException("해당 카테고리의 Q&A가 아닙니다.");
        }
        return qna;
    }

    private QnaResponse toDto(CourseQna qna) {
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
                qna.getUpdatedAt());
    }
}