package com.softwarecampus.backend.domain.board;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import com.softwarecampus.backend.domain.common.BoardCategory;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티
 */
@Entity
@Table(name = "board",
    indexes = {
        @Index(name = "idx_board_category", columnList = "category,isDeleted"),
        @Index(name = "idx_board_account", columnList = "account_id"),
        @Index(name = "idx_board_created", columnList = "createdAt")
    }
)
public class Board extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardCategory category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false)
    private int hit = 0;

    @Column(nullable = false)
    private int recommend = 0;

    @OneToMany(mappedBy = "board")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "board")
    private List<BoardAttach> attachments = new ArrayList<>();

    protected Board() {
    }

    // 연관관계 편의 메소드
    public void assignAccount(Account account) {
        this.account = account;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.assignBoard(this);
    }

    public void increaseHit() {
        this.hit++;
    }

    public void increaseRecommend() {
        this.recommend++;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public BoardCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public int getHit() {
        return hit;
    }

    public int getRecommend() {
        return recommend;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<BoardAttach> getAttachments() {
        return attachments;
    }
}
