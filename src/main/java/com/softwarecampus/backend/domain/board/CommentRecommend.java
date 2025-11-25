package com.softwarecampus.backend.domain.board;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import com.softwarecampus.backend.domain.common.BaseTimeEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CommentRecommend extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="comment_id",nullable = false)
    private Comment comment;

    //엔티티 관련 양방향 매핑 추가 2025.11.19
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="account_id",nullable = false)
    private Account account;

}
