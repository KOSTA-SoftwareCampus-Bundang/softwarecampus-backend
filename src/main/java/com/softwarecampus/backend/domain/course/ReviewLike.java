package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseTimeEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;

/**
 * 리뷰 좋아요/싫어요
 * 
 * <p>
 * 하드 삭제 정책 적용:
 * </p>
 * <ul>
 * <li>같은 타입 재클릭: DELETE (취소)</li>
 * <li>다른 타입 클릭: UPDATE (타입 변경)</li>
 * </ul>
 */
@Entity
@Table(name = "review_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "review_id", "account_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대상 리뷰
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private CourseReview review;

    // 누가 눌렀는가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LikeType type; // LIKE / DISLIKE

    // DB 레거시 컬럼 지원 (INSERT 용도)
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    public enum LikeType {
        LIKE,
        DISLIKE
    }
}
