package com.ezzyhub.controller;

import com.ezzyhub.model.*;
import com.ezzyhub.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600, allowedHeaders = "*")  // ← Make sure this line is here
public class VerificationController {

    @Autowired
    private GitHubService githubService;

    @Autowired
    private RequirementParser requirementParser;

    @Autowired
    private CodeAnalyzer codeAnalyzer;

    @Autowired
    private ImportChecker importChecker;

    @Autowired
    private RequirementMatcher requirementMatcher;

    @PostMapping("/verify")

        // ... rest of code
    public ResponseEntity<VerificationReport> verifyCode(@RequestBody VerificationRequest request) {
        try {
            System.out.println("\n=== Starting Verification ===");
            System.out.println("GitHub URL: " + request.getGithubUrl());

            // Step 1: Download code from GitHub
            System.out.println("\nStep 1: Downloading repository...");
            List<File> javaFiles;
            // Check if it's a local path (starts with C:\ or D:\ or /)
            if (request.getGithubUrl().matches("^[A-Za-z]:\\\\.*") ||
                    request.getGithubUrl().startsWith("/")) {
                javaFiles = githubService.loadFromLocal(request.getGithubUrl());
            } else {
                javaFiles = githubService.downloadAndExtract(request.getGithubUrl());
            }

            // Step 2: Parse requirements
            System.out.println("\nStep 2: Parsing requirements...");
            List<Requirement> requirements = requirementParser.parse(request.getRequirements());
            System.out.println("✓ Found " + requirements.size() + " requirements");

            // Step 3: Analyze code
            System.out.println("\nStep 3: Analyzing code...");
            List<CodeFeature> features = codeAnalyzer.analyze(javaFiles);
            System.out.println("✓ Found " + features.size() + " code features");

            // Step 4: Check imports
            System.out.println("\nStep 4: Checking imports...");
            List<ImportIssue> importIssues = importChecker.checkImports(javaFiles);
            System.out.println("✓ Found " + importIssues.size() + " import issues");

            // Step 5: Match requirements to code
            System.out.println("\nStep 5: Matching requirements...");
            requirementMatcher.matchRequirements(requirements, features);
            System.out.println("✓ Matching complete");

            // Step 6: Generate suggestions
            System.out.println("\nStep 6: Generating suggestions...");
            List<String> suggestions = requirementMatcher.generateSuggestions(requirements);
            System.out.println("✓ Generated " + suggestions.size() + " suggestions");

            // Step 7: Build report
            System.out.println("\nStep 7: Building report...");
            VerificationReport report = buildReport(requirements, importIssues, suggestions);

            System.out.println("\n=== Verification Complete ===");
            System.out.println("Coverage: " + String.format("%.2f", report.getCoveragePercentage()) + "%");
            System.out.println("==============================\n");

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            System.err.println("Error during verification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Builds the verification report
     */
    private VerificationReport buildReport(List<Requirement> requirements,
                                           List<ImportIssue> importIssues,
                                           List<String> suggestions) {
        VerificationReport report = new VerificationReport();
        report.setProjectName("Calculator Demo");
        report.setRequirements(requirements);
        report.setImportIssues(importIssues);
        report.setSuggestions(suggestions);

        // Calculate statistics
        int total = requirements.size();
        int implemented = 0;
        int partial = 0;
        int missing = 0;

        for (Requirement req : requirements) {
            switch (req.getStatus()) {
                case "IMPLEMENTED":
                    implemented++;
                    break;
                case "PARTIAL":
                    partial++;
                    break;
                case "MISSING":
                    missing++;
                    break;
            }
        }

        report.setTotalRequirements(total);
        report.setImplementedRequirements(implemented);
        report.setPartialRequirements(partial);
        report.setMissingRequirements(missing);
        report.calculateCoverage();

        return report;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("EzzyHub Backend API is running!");
    }

}