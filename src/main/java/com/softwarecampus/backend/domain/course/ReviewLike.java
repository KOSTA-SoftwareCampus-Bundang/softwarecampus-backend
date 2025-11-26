package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "review_like",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"review_id", "account_id"})
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewLike extends BaseSoftDeleteSupportEntity {

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

    public enum LikeType {
        LIKE,
        DISLIKE
    }
}

