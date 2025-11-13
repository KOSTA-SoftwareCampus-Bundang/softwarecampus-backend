package com.softwarecampus.backend.controller.academy.qna;

import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.service.academy.qna.AcademyQAService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/academies/{academyId}/qna")
@RequiredArgsConstructor
public class AcademyQAController {

    private final AcademyQAService academyQAService;

    /**
     *  Q/A 조회
     */
    @GetMapping
    public ResponseEntity<List<QAResponse>> getQAsByAcademyId(@PathVariable Long academyId) {
        List<QAResponse> response = academyQAService.getQAsByAcademyId(academyId);
        return ResponseEntity.ok(response);
    }

    /**
     *  Q/A 상세 보기
     */
    @GetMapping("/{qaId}")
    public ResponseEntity<QAResponse> getAcademyQADetail(@PathVariable Long qaId, @PathVariable Long academyId) {
        QAResponse response = academyQAService.getAcademyQADetail(qaId, academyId);
        return ResponseEntity.ok(response);
    }

    /**
     *  훈련기관 질문 등록
     */
    @PostMapping
    public ResponseEntity<QAResponse> createQuestion(@PathVariable @Positive Long academyId, @RequestBody @Valid QACreateRequest request) {
        QAResponse response = academyQAService.createQuestion(academyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     *  훈련기관 질문 수정
     */
    @PatchMapping("/{qaId}/question")
    public ResponseEntity<QAResponse> updateQuestion(@PathVariable Long qaId, @PathVariable Long academyId, @RequestBody QAUpdateRequest request) {
        QAResponse response = academyQAService.updateQuestion(academyId, qaId, request);
        return ResponseEntity.ok(response);
    }

    /**
     *  훈련기관 질문 삭제
     */
    @DeleteMapping("/{qaId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long qaId, @PathVariable Long academyId) {
        academyQAService.deleteQuestion(qaId, academyId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     *  답변 등록 / 수정
     */
    @PatchMapping("/{qaId}/answer")
    public ResponseEntity<QAResponse> updateAnswer(@PathVariable Long qaId, @PathVariable Long academyId, @RequestBody QAUpdateRequest request) {
        QAResponse response = academyQAService.updateAnswer(academyId, qaId, request);
        return ResponseEntity.ok(response);
    }

    /**
     *  답변 삭제
     */
    @DeleteMapping("/{qaId}/answer")
    public ResponseEntity<QAResponse> deleteAnswer(@PathVariable Long qaId, @PathVariable Long academyId) {
        QAResponse response = academyQAService.deleteAnswer(qaId, academyId);
        return ResponseEntity.ok(response);
    }
}
