package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import com.softwarecampus.backend.domain.user.Account;
import jakarta.persistence.*;

/**
 * 과정 즐겨찾기 엔티티
 */
@Entity
@Table(name = "course_favorite")
public class CourseFavorite extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    protected CourseFavorite() {
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Course getCourse() {
        return course;
    }
}
