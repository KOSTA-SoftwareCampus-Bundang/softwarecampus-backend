package com.softwarecampus.backend.controller.academy.qna;

import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.security.CustomUserDetails;
import com.softwarecampus.backend.service.academy.qna.AcademyQAService;
import com.softwarecampus.backend.service.academy.qna.AttachmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/academies/{academyId}/qna")
@RequiredArgsConstructor
@Slf4j
public class AcademyQAController {

    private final AcademyQAService academyQAService;
    private final AttachmentService attachmentService;

    /**
     * Q/A 첨부파일 임시로 S3에 업로드
     */
    @PostMapping("/files/upload")
    public ResponseEntity<List<QAFileDetail>> uploadQnaFiles(
            @PathVariable Long academyId,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Q/A 첨부파일 {}건 임시 업도르 요청 수신. academyId ={}", files.size(), academyId);
        List<QAFileDetail> fileDetails = attachmentService.uploadFiles(files);

        return ResponseEntity.ok(fileDetails);
    }

    /**
     * Q/A 조회 (페이징 및 검색 지원)
     */
    @GetMapping
    public ResponseEntity<Page<QAResponse>> getQAsByAcademyId(
            @PathVariable Long academyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<QAResponse> response = academyQAService.getQAsByAcademyId(academyId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Q/A 상세 보기
     */
    @GetMapping("/{qaId}")
    public ResponseEntity<QAResponse> getAcademyQADetail(@PathVariable Long qaId, @PathVariable Long academyId) {
        QAResponse response = academyQAService.getAcademyQADetail(qaId, academyId);
        return ResponseEntity.ok(response);
    }

    /**
     * 훈련기관 질문 수정
     */
    @PatchMapping("/{qaId}/question")
    public ResponseEntity<QAResponse> updateQuestion(
            @PathVariable Long qaId,
            @PathVariable Long academyId,
            @RequestBody QAUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        QAResponse response = academyQAService.updateQuestion(academyId, qaId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * 훈련기관 질문 삭제
     */
    @DeleteMapping("/{qaId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long qaId,
            @PathVariable Long academyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        academyQAService.deleteQuestion(qaId, academyId, userDetails.getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 훈련기관 답변 등록 (신규)
     */
    @PostMapping("/{qaId}/answer")
    public ResponseEntity<QAResponse> answerQuestion(
            @PathVariable Long qaId,
            @PathVariable Long academyId,
            @RequestBody QAUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        QAResponse response = academyQAService.answerQuestion(academyId, qaId, request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 훈련기관 답변 수정 (기존 답변)
     */
    @PatchMapping("/{qaId}/answer")
    public ResponseEntity<QAResponse> updateAnswer(
            @PathVariable Long qaId,
            @PathVariable Long academyId,
            @RequestBody QAUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        QAResponse response = academyQAService.updateAnswer(academyId, qaId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * 훈련기관 답변 삭제
     */
    @DeleteMapping("/{qaId}/answer")
    public ResponseEntity<QAResponse> deleteAnswer(
            @PathVariable Long qaId,
            @PathVariable Long academyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        QAResponse response = academyQAService.deleteAnswer(qaId, academyId, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
