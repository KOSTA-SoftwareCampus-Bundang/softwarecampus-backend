package com.softwarecampus.backend.domain.board;

import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_view")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class BoardView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account; // nullable - 비로그인 사용자는 null

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // nullable - 로그인 사용자는 null (IPv6 대응 45자)

    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
}
