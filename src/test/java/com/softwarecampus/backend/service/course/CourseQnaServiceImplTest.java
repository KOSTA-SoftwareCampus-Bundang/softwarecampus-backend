package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseCategory;
import com.softwarecampus.backend.domain.course.CourseQna;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.QnaAnswerRequest;
import com.softwarecampus.backend.dto.course.QnaRequest;
import com.softwarecampus.backend.dto.course.QnaResponse;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.repository.course.CourseQnaRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseQnaServiceImplTest {

    @InjectMocks
    private CourseQnaServiceImpl qnaService;

    @Mock
    private CourseQnaRepository qnaRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AccountRepository accountRepository;

    private Course testCourse;
    private CourseQna testQna;
    private Account testAccount;
    private final Long courseId = 1L;
    private final Long qnaId = 1L;
    private final Long userId = 100L;

    @BeforeEach
    void setUp() {
        CourseCategory category = CourseCategory.builder()
                .categoryType(CategoryType.JOB_SEEKER)
                .build();

        testCourse = Course.builder()
                .id(courseId)
                .name("Test Course")
                .category(category)
                .build();
        testCourse.restore();

        testAccount = mock(Account.class);
        lenient().when(testAccount.getId()).thenReturn(userId);
        lenient().when(testAccount.getUserName()).thenReturn("TestUser");

        testQna = CourseQna.builder()
                .id(qnaId)
                .course(testCourse)
                .account(testAccount)
                .title("Test Question")
                .questionText("Test Question Content")
                .build();
        testQna.restore();
    }

    @Test
    @DisplayName("QnA 목록 조회 성공")
    void getQnaList_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<CourseQna> qnaList = Arrays.asList(testQna, testQna);
        Page<CourseQna> qnaPage = new PageImpl<>(qnaList);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(qnaRepository.findByCourseId(eq(courseId), any(Pageable.class))).thenReturn(qnaPage);

        // when
        Page<QnaResponse> response = qnaService.getQnaList(courseId, null, pageable);

        // then
        assertEquals(2, response.getContent().size());
        assertEquals(testQna.getTitle(), response.getContent().get(0).title());
        verify(qnaRepository).findByCourseId(eq(courseId), any(Pageable.class));
    }

    @Test
    @DisplayName("QnA 목록 검색 성공")
    void getQnaList_search_success() {
        // given
        String keyword = "Test";
        Pageable pageable = PageRequest.of(0, 10);
        List<CourseQna> qnaList = Arrays.asList(testQna);
        Page<CourseQna> qnaPage = new PageImpl<>(qnaList);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(qnaRepository.searchByCourseIdAndKeyword(eq(courseId), eq(keyword), any(Pageable.class)))
                .thenReturn(qnaPage);

        // when
        Page<QnaResponse> response = qnaService.getQnaList(courseId, keyword, pageable);

        // then
        assertEquals(1, response.getContent().size());
        verify(qnaRepository).searchByCourseIdAndKeyword(eq(courseId), eq(keyword), any(Pageable.class));
    }

    @Test
    @DisplayName("QnA 상세 조회 성공")
    void getQnaDetail_success() {
        // given
        when(qnaRepository.findWithDetailsById(qnaId)).thenReturn(Optional.of(testQna));

        // when
        QnaResponse response = qnaService.getQnaDetail(qnaId);

        // then
        assertNotNull(response);
        assertEquals(qnaId, response.id());
    }

    @Test
    @DisplayName("질문 등록 성공")
    void createQuestion_success() {
        // given
        QnaRequest request = new QnaRequest();
        request.setTitle("New Question");
        request.setQuestionText("New Content");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(accountRepository.findById(userId)).thenReturn(Optional.of(testAccount));
        when(qnaRepository.save(any(CourseQna.class))).thenReturn(testQna);

        // when
        QnaResponse response = qnaService.createQuestion(courseId, userId, request);

        // then
        assertNotNull(response);
        verify(qnaRepository).save(any(CourseQna.class));
    }

    @Test
    @DisplayName("질문 수정 성공")
    void updateQuestion_success() {
        // given
        QnaRequest request = new QnaRequest();
        request.setTitle("Updated Title");
        request.setQuestionText("Updated Content");

        when(qnaRepository.findWithDetailsById(qnaId)).thenReturn(Optional.of(testQna));

        // when
        QnaResponse response = qnaService.updateQuestion(qnaId, userId, request);

        // then
        assertEquals("Updated Title", testQna.getTitle());
        assertEquals("Updated Content", testQna.getQuestionText());
    }

    @Test
    @DisplayName("질문 삭제 성공")
    void deleteQuestion_success() {
        // given
        when(qnaRepository.findWithDetailsById(qnaId)).thenReturn(Optional.of(testQna));

        // when
        qnaService.deleteQuestion(qnaId, userId);

        // then
        assertTrue(testQna.getIsDeleted());
    }

    @Test
    @DisplayName("답변 등록 성공")
    void answerQuestion_success() {
        // given
        QnaAnswerRequest request = new QnaAnswerRequest();
        request.setAnswerText("Answer Content");
        Long adminId = 200L;
        Account admin = mock(Account.class);

        when(qnaRepository.findWithDetailsById(qnaId)).thenReturn(Optional.of(testQna));
        when(accountRepository.findById(adminId)).thenReturn(Optional.of(admin));

        // when
        QnaResponse response = qnaService.answerQuestion(qnaId, adminId, request);

        // then
        assertEquals("Answer Content", testQna.getAnswerText());
        assertTrue(testQna.isAnswered());
    }
}
