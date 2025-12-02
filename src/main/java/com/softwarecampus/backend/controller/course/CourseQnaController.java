package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaFileDetail;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaUpdateRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;
import com.softwarecampus.backend.service.course.CourseQnaAttachmentService;
import com.softwarecampus.backend.service.course.CourseQnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
@Slf4j
public class CourseQnaController {

    private final CourseQnaService qnaService;
    private final CourseQnaAttachmentService attachmentService;

    /**
     * Q&A 첨부파일 임시 업로드 (단일 파일)
     * - 파일을 S3에 업로드하고 DB에 임시 저장
     * - Q&A 생성/수정 시 fileDetails로 전달하여 확정
     * - courseId는 과정 존재 여부 검증 및 로깅 목적으로 사용
     */
    @PostMapping("/{courseId}/qna/files/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QnaFileDetail> uploadQnaFile(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {

        log.info("Course Q&A 첨부파일 임시 업로드 요청 - courseId: {}, fileName: {}", courseId, file.getOriginalFilename());

        // courseId 유효성 검증 (존재하지 않는 과정에 대한 파일 업로드 방지)
        qnaService.validateCourseExists(courseId);

        List<QnaFileDetail> fileDetails = attachmentService.uploadFiles(List.of(file));

        // 업로드 결과 방어적 검증
        QnaFileDetail uploadedFile = fileDetails.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("파일 업로드에 실패했습니다."));

        return ResponseEntity.ok(uploadedFile);
    }

    /**
     * Q&A 첨부파일 임시 업로드 (복수 파일)
     * - 파일을 S3에 업로드하고 DB에 임시 저장
     * - Q&A 생성/수정 시 fileDetails로 전달하여 확정
     * - courseId는 과정 존재 여부 검증 및 로깅 목적으로 사용
     */
    @PostMapping("/{courseId}/qna/files/upload-multiple")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<QnaFileDetail>> uploadQnaFiles(
            @PathVariable Long courseId,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Course Q&A 첨부파일 {}건 임시 업로드 요청 - courseId: {}", files.size(), courseId);

        // courseId 유효성 검증 (존재하지 않는 과정에 대한 파일 업로드 방지)
        qnaService.validateCourseExists(courseId);

        List<QnaFileDetail> fileDetails = attachmentService.uploadFiles(files);
        return ResponseEntity.ok(fileDetails);
    }

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
    public ResponseEntity<QnaResponse> getQnaDetail(
            @PathVariable Long qnaId) {
        return ResponseEntity.ok(qnaService.getQnaDetail(qnaId));
    }

    /** 질문 등록 */
    @PostMapping("/{courseId}/qna")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QnaResponse> createQuestion(
            @PathVariable Long courseId,
            @RequestBody @Valid QnaRequest request,
            @RequestAttribute("userId") Long writerId) {
        return ResponseEntity.ok(qnaService.createQuestion(courseId, writerId, request));
    }

    /** 질문 수정 */
    @PutMapping("/qna/{qnaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QnaResponse> updateQuestion(
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaUpdateRequest request,
            @RequestAttribute("userId") Long writerId) {
        return ResponseEntity.ok(qnaService.updateQuestion(qnaId, writerId, request));
    }

    /** 질문 삭제 (질문자 본인 또는 관리자만 가능) */
    @DeleteMapping("/qna/{qnaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long qnaId,
            @RequestAttribute("userId") Long userId) {
        qnaService.deleteQuestion(qnaId, userId);
        return ResponseEntity.noContent().build();
    }

    /** 답변 등록 (관리자 또는 해당 과정 기관 담당자) */
    @PostMapping("/qna/{qnaId}/answer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMY')")
    public ResponseEntity<QnaResponse> answerQuestion(
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaAnswerRequest request,
            @RequestAttribute("userId") Long responderId) {
        return ResponseEntity.ok(qnaService.answerQuestion(qnaId, responderId, request));
    }

    /** 답변 수정 (관리자 또는 해당 과정 기관 담당자) */
    @PutMapping("/qna/{qnaId}/answer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMY')")
    public ResponseEntity<QnaResponse> updateAnswer(
            @PathVariable Long qnaId,
            @RequestBody @Valid QnaAnswerRequest request,
            @RequestAttribute("userId") Long responderId) {
        return ResponseEntity.ok(qnaService.updateAnswer(qnaId, responderId, request));
    }

    /** 답변 삭제 (관리자 또는 해당 과정 기관 담당자) */
    @DeleteMapping("/qna/{qnaId}/answer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMY')")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable Long qnaId,
            @RequestAttribute("userId") Long responderId) {
        qnaService.deleteAnswer(qnaId, responderId);
        return ResponseEntity.noContent().build();
    }
}
