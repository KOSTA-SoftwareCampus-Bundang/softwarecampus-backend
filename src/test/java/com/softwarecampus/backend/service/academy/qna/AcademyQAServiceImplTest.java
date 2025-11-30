package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.academy.academyQA.AcademyQARepository;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AcademyQAServiceImplTest {

    @InjectMocks
    private AcademyQAServiceImpl qaService;

    @Mock
    private AcademyQARepository qaRepository;

    @Mock
    private AcademyRepository academyRepository;

    @Mock
    private AttachmentService attachmentService;

    @Mock
    private AccountRepository accountRepository;

    private Academy testAcademy;
    private AcademyQA testQA;
    private final Long academyId = 1L;
    private final Long qaId = 1L;

    @BeforeEach
    void setUp() {
        testAcademy = Academy.builder()
                .id(academyId)
                .name("테스트 훈련기관")
                .build();

        // account는 이제 필수 필드
        var mockAccount = mock(com.softwarecampus.backend.domain.user.Account.class);
        lenient().when(mockAccount.getId()).thenReturn(1L);
        lenient().when(mockAccount.getUserName()).thenReturn("테스트사용자");

        testQA = AcademyQA.builder()
                .id(qaId)
                .title("테스트 질문이요")
                .questionText("테스트 질문 : 잘 돌아가나요")
                .answerText(null)
                .academy(testAcademy)
                .account(mockAccount)
                .build();
    }

    // Q/A 조회 테스트
    @Test
    @DisplayName("훈련기관 Q/A 목록 조회 성공")
    void getQAByAcademyId_success() {
        // given
        List<AcademyQA> qaList = Arrays.asList(testQA, testQA);
        Page<AcademyQA> qaPage = new PageImpl<>(qaList);
        Pageable pageable = PageRequest.of(0, 10);

        // 검색어 없는 경우
        when(qaRepository.findByAcademyId(eq(academyId), any(Pageable.class))).thenReturn(qaPage);

        // when
        Page<QAResponse> response = qaService.getQAsByAcademyId(academyId, null, pageable);

        // then
        assertEquals(2, response.getContent().size());
        assertEquals(academyId, response.getContent().get(0).getAcademyId(), "첫 번째 Q/A의 Academy ID가 일치해야 합니다.");
        verify(qaRepository, times(1)).findByAcademyId(eq(academyId), any(Pageable.class));
    }

    // Q/A 상세 보기 성공
    @Test
    @DisplayName("Q/A 상세 보기 성공")
    void getQADetail_success() {
        // given
        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        // when
        QAResponse response = qaService.getAcademyQADetail(qaId, academyId);

        // then
        assertNotNull(response);
        assertEquals(qaId, response.getId(), "Q/A ID가 일치해야 합니다.");
        assertEquals(academyId, response.getAcademyId(), "Academy ID가 일치해야 합니다.");
    }

    // Q/A 상세 보기 실패 (Academy ID 불일치)
    @Test
    @DisplayName("Q/A 상세 보기 실패")
    void getQADetail_fail_academyIdMismatch() {
        // given
        Long wrongAcademyId = 99L;
        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        // when & then
        assertThrows(com.softwarecampus.backend.exception.academy.AcademyException.class,
                () -> qaService.getAcademyQADetail(qaId, wrongAcademyId));
    }

    // 질문 등록 성공
    @Test
    @DisplayName("질문 등록 성공")
    void createQuestion_success() {
        // given
        QACreateRequest request = new QACreateRequest();
        request.setTitle("새 질문");
        request.setQuestionText("새 질문 내용");
        request.setAcademyId(academyId);
        Long userId = 100L;

        when(academyRepository.findById(academyId)).thenReturn(Optional.of(testAcademy));
        when(accountRepository.findById(userId))
                .thenReturn(Optional.of(mock(com.softwarecampus.backend.domain.user.Account.class)));
        when(qaRepository.save(any(AcademyQA.class))).thenReturn(testQA);

        // when
        QAResponse response = qaService.createQuestion(academyId, request, userId);

        // then
        assertEquals(academyId, response.getAcademyId(), "저장된 Q/A의 Academy ID가 일치해야 합니다.");
        verify(qaRepository, times(1)).save(any(AcademyQA.class));
    }

    // 질문 수정 성공
    @Test
    @DisplayName("질문 수정 성공")
    void updateQuestion_success() {
        // given
        QAUpdateRequest request = new QAUpdateRequest();
        request.setTitle("수정된 제목");
        request.setQuestionText("수정된 내용");
        Long userId = 1L; // testQA의 account ID와 일치

        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        // when
        QAResponse response = qaService.updateQuestion(academyId, qaId, request, userId);

        // then
        assertEquals("수정된 제목", response.getTitle(), "제목이 수정되어야 합니다.");
        assertEquals("수정된 내용", response.getQuestionText(), "질문 내용이 수정되어야 합니다.");

        assertEquals("수정된 제목", testQA.getTitle());
    }

    // 질문 삭제 성공
    @Test
    @DisplayName("Q/A 전체 삭제 성공 (소속 검증 통과)")
    void deleteQA_success() {
        // given
        Long userId = 100L;
        com.softwarecampus.backend.domain.user.Account mockAccount = mock(
                com.softwarecampus.backend.domain.user.Account.class);
        when(mockAccount.getId()).thenReturn(userId);
        testQA.setAccount(mockAccount);

        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        when(attachmentService.softDeleteAllByCategoryAndId(any(), any())).thenReturn(List.of());

        // when
        qaService.deleteQuestion(qaId, academyId, userId);

        // then
        verify(qaRepository, times(1)).delete(testQA);
        verify(attachmentService, times(1)).softDeleteAllByCategoryAndId(eq(AttachmentCategoryType.QNA), eq(qaId));
    }

    // 답변 등록 성공 (신규)
    @Test
    @DisplayName("답변 등록 성공 (신규)")
    void answerQuestion_success() {
        // given
        QAUpdateRequest request = new QAUpdateRequest();
        String newAnswer = "새로 작성된 답변입니다.";
        request.setAnswerText(newAnswer);
        Long userId = 100L;

        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));
        when(accountRepository.findById(userId))
                .thenReturn(Optional.of(mock(com.softwarecampus.backend.domain.user.Account.class)));

        // when
        QAResponse response = qaService.answerQuestion(academyId, qaId, request, userId);

        // then
        assertEquals(newAnswer, response.getAnswerText(), "응답 DTO의 답변 필드가 새로 작성된 답변과 일치해야 합니다.");
        assertEquals(newAnswer, testQA.getAnswerText(), "엔티티의 답변 필드가 새로 작성된 답변으로 업데이트되어야 합니다.");
    }

    // 답변 수정 성공 (기존 답변)
    @Test
    @DisplayName("답변 수정 성공 (기존 답변)")
    void updateAnswer_success() {
        // given
        QAUpdateRequest request = new QAUpdateRequest();
        String modifiedAnswer = "수정된 답변입니다.";
        request.setAnswerText(modifiedAnswer);
        Long userId = 100L;

        // 기존 답변이 있어야 수정 가능
        var mockAnswerer = mock(com.softwarecampus.backend.domain.user.Account.class);
        when(mockAnswerer.getId()).thenReturn(userId);
        testQA.setAnswerText("기존 답변입니다.");
        testQA.setAnsweredBy(mockAnswerer);

        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));
        when(accountRepository.findById(userId))
                .thenReturn(Optional.of(mockAnswerer));

        // when
        QAResponse response = qaService.updateAnswer(academyId, qaId, request, userId);

        // then
        assertEquals(modifiedAnswer, response.getAnswerText(), "응답 DTO의 답변 필드가 수정된 답변과 일치해야 합니다.");
        assertEquals(modifiedAnswer, testQA.getAnswerText(), "엔티티의 답변 필드가 수정된 답변으로 업데이트되어야 합니다.");
    }

    // 답변 삭제 실패 (답변 없음)
    @Test
    @DisplayName("답변 삭제 실패 (삭제할 답변이 없음)")
    void deleteAnswer_fail_noAnswer() {
        // given
        Long userId = 1L;
        testQA.setAnswerText(null);
        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        // when & then
        assertThrows(com.softwarecampus.backend.exception.academy.AcademyException.class,
                () -> qaService.deleteAnswer(qaId, academyId, userId),
                "삭제할 답변이 없을 경우 AcademyException이 발생해야 합니다.");

        verify(qaRepository, never()).delete(testQA);
    }
}
