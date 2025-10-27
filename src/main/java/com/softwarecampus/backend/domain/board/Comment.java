package com.softwarecampus.backend.domain.board;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 엔티티
 */
@Entity
@Table(name = "comment")
public class Comment extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment parentComment;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private Integer recommend;

    @OneToMany(mappedBy = "parentComment")
    private List<Comment> replies = new ArrayList<>();

    protected Comment() {
    }

    // 연관관계 편의 메소드
    public void assignBoard(Board board) {
        this.board = board;
    }

    public void assignParentComment(Comment parent) {
        this.parentComment = parent;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Board getBoard() {
        return board;
    }

    public Comment getParentComment() {
        return parentComment;
    }

    public String getComment() {
        return comment;
    }

    public Integer getRecommend() {
        return recommend;
    }

    public List<Comment> getReplies() {
        return replies;
    }
}
