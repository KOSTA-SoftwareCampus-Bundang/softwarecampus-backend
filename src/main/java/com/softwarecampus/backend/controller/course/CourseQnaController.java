package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;
import com.softwarecampus.backend.service.course.CourseQnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee/course")
@RequiredArgsConstructor
public class CourseQnaController {

    private final CourseQnaService qnaService;

    @GetMapping("/{courseId}/qna")
    public List<QnaResponse> getQnaList(@PathVariable Long courseId) {
        return qnaService.getQnaList(courseId);
    }

    @GetMapping("/qna/{qnaId}")
    public QnaResponse getQnaDetail(@PathVariable Long qnaId) {
        return qnaService.getQnaDetail(qnaId);
    }

    @PostMapping("/{courseId}/qna")
    public QnaResponse createQuestion(
            @PathVariable Long courseId,
            @RequestBody QnaRequest request,
            @RequestAttribute("userId") Long writerId
    ) {
        return qnaService.createQuestion(courseId, writerId, request);
    }

    @PutMapping("/qna/{qnaId}")
    public QnaResponse updateQuestion(
            @PathVariable Long qnaId,
            @RequestBody QnaRequest request,
            @RequestAttribute("userId") Long writerId
    ) {
        return qnaService.updateQuestion(qnaId, writerId, request);
    }

    @DeleteMapping("/qna/{qnaId}")
    public void deleteQuestion(
            @PathVariable Long qnaId,
            @RequestAttribute("userId") Long writerId
    ) {
        qnaService.deleteQuestion(qnaId, writerId);
    }

    @PostMapping("/qna/{qnaId}/answer")
    public QnaResponse answerQuestion(
            @PathVariable Long qnaId,
            @RequestBody QnaAnswerRequest request,
            @RequestAttribute("userId") Long adminId
    ) {
        return qnaService.answerQuestion(qnaId, adminId, request);
    }

    @PutMapping("/qna/{qnaId}/answer")
    public QnaResponse updateAnswer(
            @PathVariable Long qnaId,
            @RequestBody QnaAnswerRequest request,
            @RequestAttribute("userId") Long adminId
    ) {
        return qnaService.updateAnswer(qnaId, adminId, request);
    }

    @DeleteMapping("/qna/{qnaId}/answer")
    public void deleteAnswer(
            @PathVariable Long qnaId,
            @RequestAttribute("userId") Long adminId
    ) {
        qnaService.deleteAnswer(qnaId, adminId);
    }
}
