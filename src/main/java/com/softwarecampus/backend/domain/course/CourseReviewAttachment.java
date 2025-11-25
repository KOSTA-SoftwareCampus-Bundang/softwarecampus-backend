package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_review_attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewAttachment extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 리뷰 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private CourseReview review;

    // S3 URL
    @Column(length = 1000, nullable = false)
    private String fileUrl;

    // 파일 원본 이름
    private String originalName;

    // 파일 타입 (이수증, 기타 등 필요한 경우 enum 확장 가능)
}
