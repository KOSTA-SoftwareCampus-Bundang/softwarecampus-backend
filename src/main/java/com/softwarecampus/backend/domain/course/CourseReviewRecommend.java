package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "course_review_recommend",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"review_id", "account_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewRecommend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 추천한 리뷰 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private CourseReview review;

    /** 추천/비추천한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** true = 추천, false = 비추천 */
    @Column(nullable = false)
    private boolean liked;
}
