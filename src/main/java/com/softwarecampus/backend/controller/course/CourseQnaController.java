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
@RequestMapping("/api/{type}/course")
public class CourseQnaController {

    private final CourseQnaService qnaService;

    /** Q&A 목록 조회 (페이징 및 검색 지원) */
    @GetMapping("/{courseId}/qna")
    public ResponseEntity<Page<QnaResponse>> getQnaList(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<QnaResponse> response = qnaService.getQnaList(type, courseId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /** Q&A 상세 조회 */
    @GetMapping("/qna/{qnaId}")
    public QnaResponse getQnaDetail(
            @PathVariable CategoryType type,
            @PathVariable Long qnaId) {
        return qnaService.getQnaDetail(type, qnaId);
    }

    /** 질문 등록 */
    @PostMapping("/{courseId}/qna")
    public QnaResponse createQuestion(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @RequestBody @Valid QnaRequest request,
            @RequestAttribute("userId") Long writerId) {
        return qnaService.createQuestion(type, courseId, writerId, request);
    }

    /** 질문 수정 */
    @PutMapping("/qna/{qnaId}")
    public QnaResponse updateQuestion(
            @PathVariable CategoryType type,
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaRequest request,
            @RequestAttribute("userId") Long writerId) {
        return qnaService.updateQuestion(type, qnaId, writerId, request);
    }

    /** 질문 삭제 */
    @DeleteMapping("/qna/{qnaId}")
    public void deleteQuestion(
            @PathVariable CategoryType type,
            @PathVariable Long qnaId,
            @RequestAttribute("userId") Long writerId) {
        qnaService.deleteQuestion(type, qnaId, writerId);
    }

    /** 답변 등록 */
    @PostMapping("/qna/{qnaId}/answer")
    public QnaResponse answerQuestion(
            @PathVariable CategoryType type,
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaAnswerRequest request,
            @RequestAttribute("userId") Long adminId) {
        return qnaService.answerQuestion(type, qnaId, adminId, request);
    }

    /** 답변 수정 */
    @PutMapping("/qna/{qnaId}/answer")
    public QnaResponse updateAnswer(
            @PathVariable CategoryType type,
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaAnswerRequest request,
            @RequestAttribute("userId") Long adminId) {
        return qnaService.updateAnswer(type, qnaId, adminId, request);
    }

    /** 답변 삭제 */
    @DeleteMapping("/qna/{qnaId}/answer")
    public void deleteAnswer(
            @PathVariable CategoryType type,
            @PathVariable Long qnaId,
            @RequestAttribute("userId") Long adminId) {
        qnaService.deleteAnswer(type, qnaId, adminId);
    }
}
