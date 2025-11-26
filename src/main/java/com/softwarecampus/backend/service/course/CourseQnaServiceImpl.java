package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseQna;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.repository.course.CourseQnaRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQnaServiceImpl implements CourseQnaService {

    private final CourseQnaRepository qnaRepository;
    private final CourseRepository courseRepository;
    private final AccountRepository accountRepository;

    @Override
    public List<QnaResponse> getQnaList(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다."));
        return qnaRepository.findByCourseAndIsDeletedFalseOrderByIdDesc(course)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public QnaResponse getQnaDetail(Long qnaId) {
        CourseQna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A를 찾을 수 없습니다."));
        if(qna.getIsDeleted()) {
            throw new NotFoundException("Q&A를 찾을 수 없습니다.");
        }
        return toDto(qna);
    }

    @Override
    @Transactional
    public QnaResponse createQuestion(Long courseId, Long writerId, QnaRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다."));
        Account writer = accountRepository.findById(writerId)
                .orElseThrow(() -> new NotFoundException("작성자를 찾을 수 없습니다."));

        CourseQna qna = CourseQna.builder()
                .course(course)
                .writer(writer)
                .title(request.getTitle())
                .questionText(request.getQuestionText())
                .build();
        qnaRepository.save(qna);
        return toDto(qna);
    }

    @Override
    @Transactional
    public QnaResponse updateQuestion(Long qnaId, Long writerId, QnaRequest request) {
        CourseQna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A를 찾을 수 없습니다."));
        if (qna.getIsDeleted()) {
            throw new NotFoundException("Q&A를 찾을 수 없습니다.");
        }
        if (!qna.getWriter().getId().equals(writerId)) {
            throw new ForbiddenException("본인의 질문만 수정할 수 있습니다.");
        }
        qna.setTitle(request.getTitle());
        qna.setQuestionText(request.getQuestionText());
        return toDto(qna);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long qnaId, Long writerId) {
        CourseQna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A를 찾을 수 없습니다."));
        if (!qna.getWriter().getId().equals(writerId)) {
            throw new ForbiddenException("본인의 질문만 삭제할 수 있습니다.");
        }
        qna.markDeleted();
    }

    @Override
    @Transactional
    public QnaResponse answerQuestion(Long qnaId, Long adminId, QnaAnswerRequest request) {
        CourseQna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A를 찾을 수 없습니다."));
        if (qna.getIsDeleted()) {
            throw new NotFoundException("Q&A를 찾을 수 없습니다.");
        }
        if (qna.isAnswered()) {
            throw new ForbiddenException("이미 답변이 등록되어 있습니다.");
        }

        Account admin = accountRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("관리자를 찾을 수 없습니다."));
        qna.writeAnswer(request.getAnswerText(), admin);
        return toDto(qna);
    }

    @Override
    @Transactional
    public QnaResponse updateAnswer(Long qnaId, Long adminId, QnaAnswerRequest request) {
        CourseQna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A를 찾을 수 없습니다."));
        if (!qna.getAnsweredBy().getId().equals(adminId)) {
            throw new ForbiddenException("본인의 답변만 수정할 수 있습니다.");
        }
        if (qna.getAnsweredBy() == null) {
            throw new NotFoundException("답변이 존재하지 않습니다.");
        }
        qna.writeAnswer(request.getAnswerText(), qna.getAnsweredBy());
        return toDto(qna);
    }

    @Override
    @Transactional
    public void deleteAnswer(Long qnaId, Long adminId) {
        CourseQna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A를 찾을 수 없습니다."));
        if (!qna.getAnsweredBy().getId().equals(adminId)) {
            throw new ForbiddenException("본인의 답변만 삭제할 수 있습니다.");
        }
        if (qna.getAnsweredBy() == null) {
            throw new NotFoundException("답변이 존재하지 않습니다.");
        }
        qna.setAnswerText(null);
        qna.setAnsweredBy(null);
        qna.setAnswered(false);
    }

    // DTO 변환
    private QnaResponse toDto(CourseQna qna) {
        return new QnaResponse(
                qna.getId(),
                qna.getTitle(),
                qna.getQuestionText(),
                qna.getAnswerText(),
                qna.getWriter().getId(),
                qna.getWriter().getUserName(),
                qna.getAnsweredBy() != null ? qna.getAnsweredBy().getId() : null,
                qna.getAnsweredBy() != null ? qna.getAnsweredBy().getUserName() : null,
                qna.isAnswered(),
                qna.getCreatedAt(),
                qna.getUpdatedAt()
        );
    }
}
