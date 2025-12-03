package com.softwarecampus.backend.dto.academy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademyUpdateRequest {
    private String name;
    private String address;
    private String businessNumber;
    private String email;
    private String phoneNumber;
    private String website;
}
