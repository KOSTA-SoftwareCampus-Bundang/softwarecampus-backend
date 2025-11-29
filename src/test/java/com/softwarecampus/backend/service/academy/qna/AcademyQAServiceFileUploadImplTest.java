package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.exception.academy.AcademyException;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.academy.academyQA.AcademyQARepository;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class AcademyQAServiceFileUploadImplTest {

        @Mock
        private AcademyQARepository academyQARepository;

        @Mock
        private AcademyRepository academyRepository;

        @Mock
        private AttachmentRepository attachmentRepository;

        @Mock
        private AttachmentService attachmentService;

        @InjectMocks
        private AcademyQAServiceImpl academyQAService; // 수정된 구현체 사용

        private final Long academyId = 1L;
        private final Long qaId = 100L;
        private Academy academy;
        private AcademyQA academyQA;
        private final LocalDateTime now = LocalDateTime.now();

        @BeforeEach
        void setUp() {
                // Mock Academy 엔티티 설정
                academy = Academy.builder().id(academyId).build();

                // Mock AcademyQA 엔티티 설정 (Mock 객체 사용)
                academyQA = mock(AcademyQA.class);

                // Mocked AcademyQA의 필수 메서드 동작 설정 (QAResponse.from()에서 NPE 방지)
                when(academyQA.getId()).thenReturn(qaId);
                when(academyQA.getAcademy()).thenReturn(academy);
                when(academyQA.getCreatedAt()).thenReturn(now);
                when(academyQA.getUpdatedAt()).thenReturn(now);
                when(academyQA.getTitle()).thenReturn("Default Title");
                when(academyQA.getQuestionText()).thenReturn("Default Question");
                when(academyQA.getAnswerText()).thenReturn(null);

                // AcademyQA::updateQuestion 메서드가 호출되면 void를 반환하도록 설정
                doNothing().when(academyQA).updateQuestion(anyString(), anyString());
        }

        // -------------------------------------------------------------------------
        // Q/A 질문 등록 (파일 첨부) 테스트
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("Q/A 질문 등록 시, 첨부파일이 있으면 confirmAttachments가 호출되어야 한다")
        void createQuestion_withFiles_shouldConfirmAttachments() {
                // Given
                QAFileDetail fileDetail = QAFileDetail.builder().id(200L).build();
                QACreateRequest request = QACreateRequest.builder()
                                .title("New QA")
                                .questionText("Content")
                                .fileDetails(List.of(fileDetail)) // 파일 정보 포함
                                .build();

                when(academyRepository.findById(academyId)).thenReturn(Optional.of(academy));
                when(academyQARepository.save(any(AcademyQA.class))).thenReturn(academyQA); // savedQA 반환

                // When
                academyQAService.createQuestion(academyId, request);

                // Then
                // 1. Q/A 저장 로직 호출 검증
                verify(academyQARepository, times(1)).save(any(AcademyQA.class));

                // 2. attachmentService.confirmAttachments 호출 검증 (핵심 로직)
                // List<QAFileDetail> 인자는 Mockito의 deep equals 문제 가능성이 있어 any()로 검증의 안정성 확보
                verify(attachmentService, times(1)).confirmAttachments(
                                any(List.class),
                                eq(qaId),
                                eq(AttachmentCategoryType.QNA));
        }

        @Test
        @DisplayName("Q/A 질문 등록 시, 첨부파일이 없으면 confirmAttachments가 호출되지 않아야 한다")
        void createQuestion_withoutFiles_shouldNotCallConfirmAttachments() {
                // Given
                QACreateRequest request = QACreateRequest.builder()
                                .title("New QA Title")
                                .questionText("New QA Content")
                                .fileDetails(Collections.emptyList()) // 파일 정보 없음
                                .build();

                when(academyRepository.findById(academyId)).thenReturn(Optional.of(academy));
                when(academyQARepository.save(any(AcademyQA.class))).thenReturn(academyQA);

                // When
                academyQAService.createQuestion(academyId, request);

                // Then
                // attachmentService.confirmAttachments 호출되지 않음 검증
                verify(attachmentService, never()).confirmAttachments(any(), any(), any());
        }

        // -------------------------------------------------------------------------
        // Q/A 질문 수정 (파일 수정/삭제) 테스트
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("Q/A 수정 시, 새로운 파일이 있으면 confirmAttachments가 호출되고 삭제 로직은 호출되지 않아야 한다")
        void updateQuestion_withNewFiles_shouldConfirmAttachments() {
                // Given
                QAFileDetail newFileDetail = QAFileDetail.builder().id(300L).build();
                QAUpdateRequest request = QAUpdateRequest.builder()
                                .title("Updated Title")
                                .questionText("Updated Content")
                                .newFileDetails(List.of(newFileDetail)) // 새로운 파일 정보 포함
                                .deletedFileIds(Collections.emptyList())
                                .build();

                when(academyQARepository.findById(qaId)).thenReturn(Optional.of(academyQA));

                // When
                academyQAService.updateQuestion(academyId, qaId, request);

                // Then
                // 1. 새 첨부파일 확정 로직 검증 (호출되어야 함)
                verify(attachmentService, times(1)).confirmAttachments(
                                any(List.class),
                                eq(qaId),
                                eq(AttachmentCategoryType.QNA));

                // 2. 삭제 관련 로직은 호출되지 않아야 함
                verify(attachmentRepository, never()).findAllById(any());
                verify(attachmentService, never()).hardDeleteS3Files(any());

                // 3. 엔티티 내용 수정 메서드 호출 검증
                verify(academyQA, times(1)).updateQuestion(eq(request.getTitle()), eq(request.getQuestionText()));
        }

        @Test
        @DisplayName("Q/A 수정 시, 삭제 요청 파일이 있으면 Soft Delete 및 Hard Delete가 호출되어야 한다")
        void updateQuestion_withDeletedFiles_shouldSoftDeleteAndHardDelete() {
                // Given
                List<Long> deletedFileIds = List.of(400L, 401L);
                QAUpdateRequest request = QAUpdateRequest.builder()
                                .title("Updated Title")
                                .questionText("Updated Content")
                                .deletedFileIds(deletedFileIds) // 삭제 요청 파일 ID 포함
                                .newFileDetails(Collections.emptyList())
                                .build();

                // 🟢 Attachment 엔티티 Mock 생성 및 findAllById 결과로 반환하도록 설정
                Attachment mockAttachment1 = mock(Attachment.class);
                Attachment mockAttachment2 = mock(Attachment.class);

                when(mockAttachment1.getCategoryType()).thenReturn(AttachmentCategoryType.QNA);
                when(mockAttachment1.getCategoryId()).thenReturn(qaId);
                when(mockAttachment2.getCategoryType()).thenReturn(AttachmentCategoryType.QNA);
                when(mockAttachment2.getCategoryId()).thenReturn(qaId);

                List<Attachment> attachmentsToProcess = List.of(mockAttachment1, mockAttachment2);

                when(academyQARepository.findById(qaId)).thenReturn(Optional.of(academyQA));
                when(attachmentRepository.findAllById(deletedFileIds)).thenReturn(attachmentsToProcess);

                // When
                academyQAService.updateQuestion(academyId, qaId, request);

                // Then
                // 1. attachmentRepository.findAllById 호출 검증
                verify(attachmentRepository, times(1)).findAllById(eq(deletedFileIds));

                // 2. 각 Attachment 엔티티의 softDelete() 로직 호출 검증 (DB Soft Delete)
                verify(mockAttachment1, times(1)).softDelete();
                verify(mockAttachment2, times(1)).softDelete();

                // 3. attachmentService.hardDeleteS3Files 호출 검증 (S3 물리 삭제)
                verify(attachmentService, times(1)).hardDeleteS3Files(eq(attachmentsToProcess));

                // 4. 새 파일 확정 로직은 호출되지 않아야 함
                verify(attachmentService, never()).confirmAttachments(any(), any(), any());
        }

        @Test
        @DisplayName("Q/A 수정 시, 새로운 파일과 삭제 파일이 모두 없으면 파일 관련 서비스는 호출되지 않아야 한다")
        void updateQuestion_withoutAnyFileChanges_shouldNotCallAttachmentServices() {
                // Given
                QAUpdateRequest request = QAUpdateRequest.builder()
                                .title("Updated Title")
                                .questionText("Updated Content")
                                .newFileDetails(Collections.emptyList())
                                .deletedFileIds(Collections.emptyList())
                                .build();

                when(academyQARepository.findById(qaId)).thenReturn(Optional.of(academyQA));

                // When
                academyQAService.updateQuestion(academyId, qaId, request);

                // Then
                // 파일 확정 및 삭제 로직 모두 호출되지 않음 검증
                verify(attachmentService, never()).confirmAttachments(any(), any(), any());
                verify(attachmentRepository, never()).findAllById(any());
                verify(attachmentService, never()).hardDeleteS3Files(any());
        }

        // -------------------------------------------------------------------------
        // Q/A 질문 삭제 (파일 전체 삭제) 테스트
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("Q/A 질문 삭제 시, 연결된 모든 파일에 대해 Soft Delete 및 Hard Delete가 호출되어야 한다")
        void deleteQuestion_shouldSoftDeleteAndHardDeleteAllAttachments() {
                // Given
                when(academyQARepository.findById(qaId)).thenReturn(Optional.of(academyQA));

                List<Attachment> attachments = List.of(
                                Attachment.builder().categoryId(500L).filename("s3/file1").build());

                // softDeleteAllByCategoryAndId 호출 시, S3 Hard Delete를 위한 목록 반환 Mocking
                when(attachmentService.softDeleteAllByCategoryAndId(
                                eq(AttachmentCategoryType.QNA),
                                eq(qaId))).thenReturn(attachments);

                // When
                academyQAService.deleteQuestion(qaId, academyId);

                // Then
                // 1. softDeleteAllByCategoryAndId 호출 검증 (DB Soft Delete)
                verify(attachmentService, times(1)).softDeleteAllByCategoryAndId(
                                eq(AttachmentCategoryType.QNA),
                                eq(qaId));

                // 2. hardDeleteS3Files 호출 검증 (S3 Hard Delete)
                verify(attachmentService, times(1)).hardDeleteS3Files(eq(attachments));

                // 3. 최종적으로 AcademyQA 레코드 삭제 검증
                verify(academyQARepository, times(1)).delete(eq(academyQA));
        }
}