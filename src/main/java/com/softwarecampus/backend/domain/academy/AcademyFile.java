package com.softwarecampus.backend.domain.academy;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 기관 등록 첨부파일 엔티티
 * 사업자등록증, 교육기관 인증서 등의 파일 메타데이터를 저장
 * 실제 파일은 AWS S3에 저장되며, 이 엔티티는 S3 URL과 키를 관리
 */
@Entity
@Table(name = "academy_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AcademyFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 기관 (다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    /**
     * 사용자가 업로드한 원본 파일명
     * 예: "사업자등록증.pdf"
     */
    @Column(nullable = false, length = 255)
    private String originalFileName;

    /**
     * S3 파일 접근 URL
     * 예: "https://bucket-name.s3.ap-northeast-2.amazonaws.com/academy/1/uuid-file.pdf"
     */
    @Column(nullable = false, length = 1000)
    private String fileUrl;

    /**
     * S3 객체 키 (버킷 내 경로)
     * 예: "academy/1/a1b2c3d4-business-registration.pdf"
     */
    @Column(nullable = false, length = 500)
    private String s3Key;

    /**
     * 파일 크기 (bytes)
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * MIME 타입
     * 예: "application/pdf", "image/jpeg"
     */
    @Column(nullable = false, length = 100)
    private String contentType;

    /**
     * 파일 업로드 일시 (자동 생성)
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
