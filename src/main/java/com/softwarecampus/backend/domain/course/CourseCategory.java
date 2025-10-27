package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import com.softwarecampus.backend.domain.common.CourseCategoryType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 과정 카테고리 엔티티
 */
@Entity
@Table(name = "course_category")
public class CourseCategory extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name")
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private CourseCategoryType categoryType;

    @OneToMany(mappedBy = "courseCategory")
    private List<Course> courses = new ArrayList<>();

    protected CourseCategory() {
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public CourseCategoryType getCategoryType() {
        return categoryType;
    }

    public List<Course> getCourses() {
        return courses;
    }
}
