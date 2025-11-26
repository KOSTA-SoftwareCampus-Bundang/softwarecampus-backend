package com.softwarecampus.backend.controller.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{type}/course")
public class CourseQnaController {

    // --------------------------------------------------
    // Q/A 조회 (리스트)
    // GET /api/{type}/course/{courseId}/qna
    // --------------------------------------------------
    @GetMapping("/{courseId}/qna")
    public ResponseEntity<?> getQnaList(
            @PathVariable String type,
            @PathVariable Long courseId) {
        return ResponseEntity.ok().build();
    }

    // --------------------------------------------------
    // Q/A 상세보기
    // GET /api/{type}/course/qna/{qnaId}
    // --------------------------------------------------
    @GetMapping("/qna/{qnaId}")
    public ResponseEntity<?> getQnaDetail(
            @PathVariable String type,
            @PathVariable Long qnaId) {
        return ResponseEntity.ok().build();
    }

    // --------------------------------------------------
    // Q/A 질문 등록
    // POST /api/{type}/course/{courseId}/qna
    // --------------------------------------------------
    @PostMapping("/{courseId}/qna")
    public ResponseEntity<?> createQuestion(
            @PathVariable String type,
            @PathVariable Long courseId,
            @RequestBody Object request) {
        return ResponseEntity.ok().build();
    }

    // --------------------------------------------------
    // Q/A 질문 수정
    // PUT /api/{type}/course/qna/{qnaId}
    // --------------------------------------------------
    @PutMapping("/qna/{qnaId}")
    public ResponseEntity<?> updateQuestion(
            @PathVariable String type,
            @PathVariable Long qnaId,
            @RequestBody Object request) {
        return ResponseEntity.ok().build();
    }

    // --------------------------------------------------
    // Q/A 질문 삭제
    // DELETE /api/{type}/course/qna/{qnaId}
    // --------------------------------------------------
    @DeleteMapping("/qna/{qnaId}")
    public ResponseEntity<?> deleteQuestion(
            @PathVariable String type,
            @PathVariable Long qnaId) {
        return ResponseEntity.ok().build();
    }

    // --------------------------------------------------
    // Q/A 답변 등록
    // POST /api/{type}/course/qna/{qnaId}/answer
    // --------------------------------------------------
    @PostMapping("/qna/{qnaId}/answer")
    public ResponseEntity<?> answerQuestion(
            @PathVariable String type,
            @PathVariable Long qnaId,
            @RequestBody Object request) {
        return ResponseEntity.ok().build();
    }

    // --------------------------------------------------
    // Q/A 답변 수정
    // PUT /api/{type}/course/qna/{qnaId}/answer
    // --------------------------------------------------
    @PutMapping("/qna/{qnaId}/answer")
    public ResponseEntity<?> updateAnswer(
            @PathVariable String type,
            @PathVariable Long qnaId,
            @RequestBody Object request) {
        return ResponseEntity.ok().build();
    }

    // --------------------------------------------------
    // Q/A 답변 삭제
    // DELETE /api/{type}/course/qna/{qnaId}/answer
    // --------------------------------------------------
    @DeleteMapping("/qna/{qnaId}/answer")
    public ResponseEntity<?> deleteAnswer(
            @PathVariable String type,
            @PathVariable Long qnaId) {
        return ResponseEntity.ok().build();
    }
}
