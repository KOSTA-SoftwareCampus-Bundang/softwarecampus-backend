package com.softwarecampus.backend.domain.academy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "academy")
public class Academy {
    @Id
    private Long id;
}