package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;
import com.softwarecampus.backend.service.course.CourseQnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseQnaController {

    private final CourseQnaService qnaService;

    /** Q&A 목록 조회 (페이징 및 검색 지원) */
    @GetMapping("/{courseId}/qna")
    public ResponseEntity<Page<QnaResponse>> getQnaList(
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<QnaResponse> response = qnaService.getQnaList(courseId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /** Q&A 상세 조회 */
    @GetMapping("/qna/{qnaId}")
    public QnaResponse getQnaDetail(
            @PathVariable Long qnaId) {
        return qnaService.getQnaDetail(qnaId);
    }

    /** 질문 등록 */
    @PostMapping("/{courseId}/qna")
    public QnaResponse createQuestion(
            @PathVariable Long courseId,
            @RequestBody @Valid QnaRequest request,
            @RequestAttribute("userId") Long writerId) {
        return qnaService.createQuestion(courseId, writerId, request);
    }

    /** 질문 수정 */
    @PutMapping("/qna/{qnaId}")
    public QnaResponse updateQuestion(
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaRequest request,
            @RequestAttribute("userId") Long writerId) {
        return qnaService.updateQuestion(qnaId, writerId, request);
    }

    /** 질문 삭제 */
    @DeleteMapping("/qna/{qnaId}")
    public void deleteQuestion(
            @PathVariable Long qnaId,
            @RequestAttribute("userId") Long writerId) {
        qnaService.deleteQuestion(qnaId, writerId);
    }

    /** 답변 등록 */
    @PostMapping("/qna/{qnaId}/answer")
    public QnaResponse answerQuestion(
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaAnswerRequest request,
            @RequestAttribute("userId") Long adminId) {
        return qnaService.answerQuestion(qnaId, adminId, request);
    }

    /** 답변 수정 */
    @PutMapping("/qna/{qnaId}/answer")
    public QnaResponse updateAnswer(
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaAnswerRequest request,
            @RequestAttribute("userId") Long adminId) {
        return qnaService.updateAnswer(qnaId, adminId, request);
    }

    /** 답변 삭제 */
    @DeleteMapping("/qna/{qnaId}/answer")
    public void deleteAnswer(
            @PathVariable Long qnaId,
            @RequestAttribute("userId") Long adminId) {
        qnaService.deleteAnswer(qnaId, adminId);
    }
}
