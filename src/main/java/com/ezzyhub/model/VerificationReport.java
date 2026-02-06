package com.ezzyhub.model;

import lombok.Data;
import java.util.List;

@Data
public class VerificationReport {
    private String projectName;
    private int totalRequirements;
    private int implementedRequirements;
    private int partialRequirements;
    private int missingRequirements;
    private double coveragePercentage;

    private List<Requirement> requirements;
    private List<ImportIssue> importIssues;
    private List<String> suggestions;

    public void calculateCoverage() {
        if (totalRequirements > 0) {
            double implemented = implementedRequirements + (partialRequirements * 0.5);
            this.coveragePercentage = (implemented / totalRequirements) * 100;
        }
    }
}