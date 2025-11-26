package com.softwarecampus.backend.domain.academy.qna;

import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "file")
public class Attachment extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originName;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentCategoryType categoryType;

    @Column
    private Long categoryId;

    @Builder
    public Attachment(String originName, String filename, AttachmentCategoryType categoryType, Long categoryId) {
        this.originName = originName;
        this.filename = filename;
        this.categoryType = categoryType;
        this.categoryId = categoryId;
    }

    public void updateCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void updateCategoryType(AttachmentCategoryType categoryType) {
        this.categoryType = categoryType;
    }
}
