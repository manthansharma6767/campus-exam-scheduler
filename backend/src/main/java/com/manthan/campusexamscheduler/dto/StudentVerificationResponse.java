package com.manthan.campusexamscheduler.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentVerificationResponse {

    private String token;

    private long expiresInSeconds;
}
