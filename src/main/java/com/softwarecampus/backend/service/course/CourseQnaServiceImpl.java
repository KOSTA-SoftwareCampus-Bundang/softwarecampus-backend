package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQnaServiceImpl implements CourseQnaService {

    private final CourseQnaRepository qnaRepository;
    private final CourseRepository courseRepository;
    private final AccountRepository accountRepository;

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
        throw new ForbiddenException("본인의 답변만 삭제할 수 있습니다.");
    }qna.setAnswerText(null);qna.setAnsweredBy(null);qna.setAnswered(false);

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