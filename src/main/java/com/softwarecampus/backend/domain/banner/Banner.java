package com.softwarecampus.backend.domain.banner;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배너 엔티티
 * 수정일: 2025-12-02 - 코드 스타일 통일, 특정 목적 메서드 추가
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Banner extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column
    private String imageUrl;

    private String linkUrl;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private Boolean isActivated;

    /**
     * 배너 정보 업데이트 (전체 필드)
     */
    public void update(String title, String imageUrl, String linkUrl, String description, int sequence,
            Boolean isActivated) {
        if (title != null) {
            this.title = title;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (linkUrl != null) {
            this.linkUrl = linkUrl;
        }
        if (description != null) {
            this.description = description;
        }

        this.sequence = sequence;

        if (isActivated != null) {
            this.isActivated = isActivated;
        }
    }

    /**
     * 배너 순서 업데이트 (특정 목적 메서드)
     * 작성일: 2025-12-02
     */
    public void updateSequence(int newSequence) {
        this.sequence = newSequence;
    }

    /**
     * 배너 활성화 상태 토글 (특정 목적 메서드)
     * 작성일: 2025-12-02
     */
    public void toggleActivation() {
        this.isActivated = !this.isActivated;
    }
}
