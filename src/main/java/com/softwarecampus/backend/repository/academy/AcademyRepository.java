package com.softwarecampus.backend.repository.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademyRepository extends JpaRepository<Academy, Long> {

}
