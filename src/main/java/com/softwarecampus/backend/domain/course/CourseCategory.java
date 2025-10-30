package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCategory extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType categoryType; // EMPLOYEE, JOB_SEEKER

}
