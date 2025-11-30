package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.academy.academyQA.AcademyQARepository;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

        @Mock
        private AccountRepository accountRepository;

        @InjectMocks
        private AcademyQAServiceImpl academyQAService;

        private final Long academyId = 1L;
        private final Long qaId = 100L;
        private final Long userId = 100L;
        private Academy academy;
        private AcademyQA academyQA;
        private final LocalDateTime now = LocalDateTime.now();

        @BeforeEach
        void setUp() {
                academy = Academy.builder().id(academyId).build();
                academyQA = mock(AcademyQA.class);

                when(academyQA.getId()).thenReturn(qaId);
                when(academyQA.getAcademy()).thenReturn(academy);
                when(academyQA.getCreatedAt()).thenReturn(now);
                when(academyQA.getUpdatedAt()).thenReturn(now);
                when(academyQA.getTitle()).thenReturn("Default Title");
                when(academyQA.getQuestionText()).thenReturn("Default Question");
                when(academyQA.getAnswerText()).thenReturn(null);

                doNothing().when(academyQA).updateQuestion(anyString(), anyString());

                Account account = mock(Account.class);
                when(account.getId()).thenReturn(userId);
                when(academyQA.getAccount()).thenReturn(account);
                when(accountRepository.findById(anyLong())).thenReturn(Optional.of(account));
        }

        @Test
        @DisplayName("Q/A 질문 등록 시, 첨부파일이 있으면 confirmAttachments가 호출되어야 한다")
        void createQuestion_withFiles_shouldConfirmAttachments() {
                // given
                QAFileDetail fileDetail = QAFileDetail.builder().id(200L).build();
                QACreateRequest request = QACreateRequest.builder()
                                .title("New QA")
                                .questionText("Content")
                                .fileDetails(List.of(fileDetail))
                                .build();

                when(academyRepository.findById(academyId)).thenReturn(Optional.of(academy));
                when(academyQARepository.save(any(AcademyQA.class))).thenReturn(academyQA);

                // when
                academyQAService.createQuestion(academyId, request, userId);

                // then
                verify(academyQARepository, times(1)).save(any(AcademyQA.class));
                verify(attachmentService, times(1)).confirmAttachments(
                                anyList(),
                                eq(qaId),
                                eq(AttachmentCategoryType.QNA));
        }

        @Test
        @DisplayName("Q/A 질문 등록 시, 첨부파일이 없으면 confirmAttachments가 호출되지 않아야 한다")
        void createQuestion_withoutFiles_shouldNotCallConfirmAttachments() {
                // given
                QACreateRequest request = QACreateRequest.builder()
                                .title("New QA Title")
                                .questionText("New QA Content")
                                .fileDetails(Collections.emptyList())
                                .build();

                when(academyRepository.findById(academyId)).thenReturn(Optional.of(academy));
                when(academyQARepository.save(any(AcademyQA.class))).thenReturn(academyQA);

                // when
                academyQAService.createQuestion(academyId, request, userId);

                // then
                verify(attachmentService, never()).confirmAttachments(any(), any(), any());
        }

        @Test
        @DisplayName("Q/A 수정 시, 새로운 파일이 있으면 confirmAttachments가 호출되고 삭제 로직은 호출되지 않아야 한다")
        void updateQuestion_withNewFiles_shouldConfirmAttachments() {
                // given
                QAFileDetail newFileDetail = QAFileDetail.builder().id(300L).build();
                QAUpdateRequest request = QAUpdateRequest.builder()
                                .title("Updated Title")
                                .questionText("Updated Content")
                                .newFileDetails(List.of(newFileDetail))
                                .deletedFileIds(Collections.emptyList())
                                .build();

                when(academyQARepository.findById(qaId)).thenReturn(Optional.of(academyQA));

                // when
                academyQAService.updateQuestion(academyId, qaId, request, userId);

                // then
                verify(attachmentService, times(1)).confirmAttachments(
                                anyList(),
                                eq(qaId),
                                eq(AttachmentCategoryType.QNA));

                verify(attachmentRepository, never()).findAllById(any());
                verify(attachmentService, never()).hardDeleteS3Files(any());
                verify(academyQA, times(1)).updateQuestion(eq(request.getTitle()), eq(request.getQuestionText()));
        }

        @Test
        @DisplayName("Q/A 수정 시, 삭제 요청 파일이 있으면 Soft Delete 및 Hard Delete가 호출되어야 한다")
        void updateQuestion_withDeletedFiles_shouldSoftDeleteAndHardDelete() {
                // given
                List<Long> deletedFileIds = List.of(400L, 401L);
                QAUpdateRequest request = QAUpdateRequest.builder()
                                .title("Updated Title")
                                .questionText("Updated Content")
                                .deletedFileIds(deletedFileIds)
                                .newFileDetails(Collections.emptyList())
                                .build();

                Attachment mockAttachment1 = mock(Attachment.class);
                Attachment mockAttachment2 = mock(Attachment.class);

                when(mockAttachment1.getCategoryType()).thenReturn(AttachmentCategoryType.QNA);
                when(mockAttachment1.getCategoryId()).thenReturn(qaId);
                when(mockAttachment2.getCategoryType()).thenReturn(AttachmentCategoryType.QNA);
                when(mockAttachment2.getCategoryId()).thenReturn(qaId);

                List<Attachment> attachmentsToProcess = List.of(mockAttachment1, mockAttachment2);

                when(academyQARepository.findById(qaId)).thenReturn(Optional.of(academyQA));
                when(attachmentRepository.findAllById(deletedFileIds)).thenReturn(attachmentsToProcess);

                // when
                academyQAService.updateQuestion(academyId, qaId, request, userId);

                // then
                verify(attachmentRepository, times(1)).findAllById(eq(deletedFileIds));
                verify(mockAttachment1, times(1)).softDelete();
                verify(mockAttachment2, times(1)).softDelete();
                verify(attachmentService, times(1)).hardDeleteS3Files(eq(attachmentsToProcess));
                verify(attachmentService, never()).confirmAttachments(any(), any(), any());
        }

        @Test
        @DisplayName("Q/A 수정 시, 새로운 파일과 삭제 파일이 모두 없으면 파일 관련 서비스는 호출되지 않아야 한다")
        void updateQuestion_withoutAnyFileChanges_shouldNotCallAttachmentServices() {
                // given
                QAUpdateRequest request = QAUpdateRequest.builder()
                                .title("Updated Title")
                                .questionText("Updated Content")
                                .newFileDetails(Collections.emptyList())
                                .deletedFileIds(Collections.emptyList())
                                .build();

                when(academyQARepository.findById(qaId)).thenReturn(Optional.of(academyQA));

                // when
                academyQAService.updateQuestion(academyId, qaId, request, userId);

                // then
                verify(attachmentService, never()).confirmAttachments(any(), any(), any());
                verify(attachmentRepository, never()).findAllById(any());
                verify(attachmentService, never()).hardDeleteS3Files(any());
        }

        @Test
        @DisplayName("Q/A 질문 삭제 시, 연결된 모든 파일에 대해 Soft Delete 및 Hard Delete가 호출되어야 한다")
        void deleteQuestion_shouldSoftDeleteAndHardDeleteAllAttachments() {
                // given
                Account mockAccount = mock(Account.class);
                when(mockAccount.getId()).thenReturn(userId);
                when(academyQA.getAccount()).thenReturn(mockAccount);

                when(academyQARepository.findById(qaId)).thenReturn(Optional.of(academyQA));

                List<Attachment> attachments = List.of(
                                Attachment.builder().categoryId(500L).filename("s3/file1").build());

                when(attachmentService.softDeleteAllByCategoryAndId(
                                eq(AttachmentCategoryType.QNA),
                                eq(qaId))).thenReturn(attachments);

                // when
                academyQAService.deleteQuestion(qaId, academyId, userId);

                // then
                verify(attachmentService, times(1)).softDeleteAllByCategoryAndId(
                                eq(AttachmentCategoryType.QNA),
                                eq(qaId));

                verify(attachmentService, times(1)).hardDeleteS3Files(eq(attachments));
                verify(academyQARepository, times(1)).delete(eq(academyQA));
        }
}