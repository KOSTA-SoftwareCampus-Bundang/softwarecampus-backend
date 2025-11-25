package com.softwarecampus.backend.domain.board;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Board extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BoardCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long hits;

    @Column(name="is_secret",nullable = false)
    private boolean secret;

    @OneToMany(mappedBy = "board")
    @Builder.Default
    @BatchSize(size = 100)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "board",cascade = CascadeType.ALL)
    @Builder.Default
    @BatchSize(size = 100)
    private List<BoardAttach> boardAttaches = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<BoardRecommend> boardRecommends = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
