package com.softwarecampus.backend.domain.banner;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Banner extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column
    private String imageUrl;

    private String linkUrl;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private Boolean isActivated;

    public void update(String title, String imageUrl, String linkUrl, String description, int sequence,
            Boolean isActivated) {
        if (title != null)
            this.title = title;
        if (imageUrl != null)
            this.imageUrl = imageUrl;
        if (linkUrl != null)
            this.linkUrl = linkUrl;
        if (description != null)
            this.description = description;

        this.sequence = sequence;
        if (isActivated != null)
            this.isActivated = isActivated;
    }
}
