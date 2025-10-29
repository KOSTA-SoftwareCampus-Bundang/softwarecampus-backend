package com.softwarecampus.backend.domain.board;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;
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

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long hits;

    @Column(nullable = false)
    private boolean isSecret;

    @OneToMany(mappedBy ="board")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy ="board")
    private List<BoardAttach> boardAttaches = new ArrayList<>();

    @OneToMany(mappedBy = "board",cascade = CascadeType.REMOVE)
    private List<BoardRecommend> boardRecommends = new ArrayList<>();

}
