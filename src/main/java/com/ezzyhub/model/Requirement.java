package com.ezzyhub.model;

import lombok.Data;
import java.util.List;

@Data
public class Requirement {
    private String id;              // REQ-ADD-1
    private String text;            // Full requirement text
    private List<String> keywords;  // Extracted keywords
    private String status;          // IMPLEMENTED, PARTIAL, MISSING
    private double matchScore;      // 0.0 - 1.0
    private String matchedFeature;  // Which code feature implements it
}