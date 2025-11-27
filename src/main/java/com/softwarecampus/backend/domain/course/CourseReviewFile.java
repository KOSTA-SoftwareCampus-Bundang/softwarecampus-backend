package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_review_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewFile extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reviewId; // 연관된 CourseReview의 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private Account uploader; // 업로더

    @Column(nullable = false)
    private String fileUrl;

    /** Soft delete, BaseSoftDeleteSupportEntity 상속 */
    public void markDeleted() {
        super.markDeleted();
    }

    public void restore() {
        super.restore();
    }
}
