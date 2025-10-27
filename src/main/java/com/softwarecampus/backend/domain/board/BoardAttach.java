package com.softwarecampus.backend.domain.board;

import jakarta.persistence.*;

/**
 * 게시글 첨부파일 엔티티
 */
@Entity
@Table(name = "board_attach")
public class BoardAttach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    private String filename;
    private String originalFilename;
    private String filePath;

    protected BoardAttach() {
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Board getBoard() {
        return board;
    }

    public String getFilename() {
        return filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getFilePath() {
        return filePath;
    }
}
