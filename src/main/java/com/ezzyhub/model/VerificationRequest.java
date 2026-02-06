package com.ezzyhub.model;

import lombok.Data;

@Data
public class VerificationRequest {
    private String githubUrl;
    private String requirements;
}