package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import com.softwarecampus.backend.dto.academy.qna.QACreateRequest;
import com.softwarecampus.backend.dto.academy.qna.QAResponse;
import com.softwarecampus.backend.dto.academy.qna.QAUpdateRequest;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.academy.academyQA.AcademyQARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*; // Assertions 클래스의 모든 정적 메서드 import
import static org.mockito.ArgumentMatchers.any;
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
        testQA = AcademyQA.builder()
                .id(qaId)
                .title("테스트 질문이요")
                .questionText("테스트 질문 : 잘 돌아가나요")
                .answerText(null)
                .academy(testAcademy)
                .build();
    }

    // Q/A 조회 테스트
    @Test
    @DisplayName("훈련기관 Q/A 목록 조회 성공")
    void getQAByAcademyId_success() {
        List<AcademyQA> qaList = Arrays.asList(testQA, testQA);
        when(qaRepository.findAllByAcademyId(academyId)).thenReturn(qaList);

        List<QAResponse> response = qaService.getQAsByAcademyId(academyId);

        assertEquals(2, response.size());
        assertEquals(academyId, response.get(0).getAcademyId(), "첫 번째 Q/A의 Academy ID가 일치해야 합니다.");
        verify(qaRepository, times(1)).findAllByAcademyId(academyId);
    }

    @Test
    @DisplayName("Q/A 상세 보기 성공")
    void getQADetail_success() {
        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));
        QAResponse response = qaService.getAcademyQADetail(qaId, academyId);

        assertNotNull(response);
        assertEquals(qaId, response.getId(), "Q/A ID가 일치해야 합니다.");
        assertEquals(academyId, response.getAcademyId(), "Academy ID가 일치해야 합니다.");
    }

    @Test
    @DisplayName("Q/A 상세 보기 실패")
    void getQADetail_fail_academyIdMismatch() {
        Long wrongAcademyId = 99L;
        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        assertThrows(IllegalArgumentException.class, () -> qaService.getAcademyQADetail(qaId, wrongAcademyId));
    }

    @Test
    @DisplayName("질문 등록 성공")
    void createQuestion_success() {
        QACreateRequest request = new  QACreateRequest();
        request.setTitle("새 질문");
        request.setQuestionText("새 질문 내용");
        request.setAcademyId(academyId);

        when(academyRepository.findById(academyId)).thenReturn(Optional.of(testAcademy));
        when(qaRepository.save(any(AcademyQA.class))).thenReturn(testQA);

        QAResponse response = qaService.createQuestion(academyId, request);

        assertEquals(academyId, response.getAcademyId(), "저장된 Q/A의 Academy ID가 일치해야 합니다.");
        verify(qaRepository, times(1)).save(any(AcademyQA.class));
    }

    @Test
    @DisplayName("질문 수정 성공")
    void updateQuestion_success() {
        QAUpdateRequest request = new QAUpdateRequest();
        request.setTitle("수정된 제목");
        request.setQuestionText("수정된 내용");

        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        QAResponse response = qaService.updateQuestion(academyId, qaId, request);

        assertEquals("수정된 제목", response.getTitle(), "제목이 수정되어야 합니다.");
        assertEquals("수정된 내용", response.getQuestionText(), "질문 내용이 수정되어야 합니다.");

        assertEquals("수정된 제목", testQA.getTitle());
    }

    @Test
    @DisplayName("Q/A 전체 삭제 성공 (소속 검증 통과)")
    void deleteQA_success() {
        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        qaService.deleteQuestion(qaId, academyId);

        verify(qaRepository, times(1)).delete(testQA);
    }

    @Test
    @DisplayName("답변 등록/수정 성공")
    void updateAnswer_success() {
        QAUpdateRequest request = new QAUpdateRequest();
        String newAnswer = "새로 작성된 답변입니다.";
        request.setAnswerText(newAnswer);

        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        QAResponse response = qaService.updateAnswer(academyId, qaId, request);

        assertEquals(newAnswer, response.getAnswerText(), "응답 DTO의 답변 필드가 새로 작성된 답변과 일치해야 합니다.");
        assertEquals(newAnswer, testQA.getAnswerText(), "엔티티의 답변 필드가 새로 작성된 답변으로 업데이트되어야 합니다.");
    }

    @Test
    @DisplayName("답변 삭제 실패 (삭제할 답변이 없음)")
    void deleteAnswer_fail_noAnswer() {
        testQA.setAnswerText(null);
        when(qaRepository.findById(qaId)).thenReturn(Optional.of(testQA));

        assertThrows(IllegalStateException.class, () -> qaService.deleteAnswer(qaId, academyId),
                "삭제할 답변이 없을 경우 IllegalStateException이 발생해야 합니다.");

        verify(qaRepository, never()).delete(testQA);
    }
}
