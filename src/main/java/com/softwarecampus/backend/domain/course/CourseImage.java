package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseImage extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000, nullable = false)
    private String imageUrl;

    /** @deprecated isThumbnail 대신 imageType 사용 권장 */
    @Deprecated
    private boolean isThumbnail;

    /** 이미지 타입 (THUMBNAIL, HEADER, CONTENT) */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private CourseImageType imageType = CourseImageType.THUMBNAIL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(length = 255)
    private String originalFilename;

    /**
     * 썸네일 여부 확인 (하위 호환성)
     */
    public boolean isThumbnail() {
        return imageType == CourseImageType.THUMBNAIL || isThumbnail;
    }

    /**
     * 헤더 이미지 여부 확인
     */
    public boolean isHeader() {
        return imageType == CourseImageType.HEADER;
    }

    /**
     * 콘텐츠 이미지 여부 확인
     */
    public boolean isContent() {
        return imageType == CourseImageType.CONTENT;
    }
}
