package com.softwarecampus.backend.domain.course;

import jakarta.persistence.*;

/**
 * 과정 태그 엔티티
 */
@Entity
@Table(name = "course_tag")
public class CourseTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    protected CourseTag() {
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
