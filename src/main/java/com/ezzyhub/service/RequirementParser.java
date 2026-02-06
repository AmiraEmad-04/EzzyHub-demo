package com.ezzyhub.service;

import com.ezzyhub.model.Requirement;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RequirementParser {

    // Updated pattern to match REQ-XXX-N OR REQ-NNN formats
    private static final Pattern REQ_PATTERN = Pattern.compile("(REQ-[A-Z0-9]+-\\d+|REQ-\\d+)");

    /**
     * Parses requirements text and extracts individual requirements
     */
    public List<Requirement> parse(String requirementText) {
        System.out.println("========================================");
        System.out.println("Parsing requirements...");
        System.out.println("Input text length: " + requirementText.length());
        System.out.println("Input text: " + requirementText);
        System.out.println("========================================");

        List<Requirement> requirements = new ArrayList<>();

        if (requirementText == null || requirementText.trim().isEmpty()) {
            System.err.println("ERROR: Empty requirement text!");
            return requirements;
        }

        // Split by lines
        String[] lines = requirementText.split("\\r?\\n");
        System.out.println("Found " + lines.length + " lines");

        Requirement currentReq = null;
        StringBuilder currentText = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            System.out.println("Processing line: " + line);

            if (line.isEmpty()) {
                continue;
            }

            // Check if line contains a requirement ID
            Matcher matcher = REQ_PATTERN.matcher(line);

            if (matcher.find()) {
                // Save previous requirement
                if (currentReq != null) {
                    String fullText = currentText.toString().trim();
                    currentReq.setText(fullText);
                    currentReq.setKeywords(extractKeywords(fullText));
                    currentReq.setStatus("PENDING");
                    requirements.add(currentReq);
                    System.out.println("✓ Added requirement: " + currentReq.getId());
                }

                // Start new requirement
                currentReq = new Requirement();
                currentReq.setId(matcher.group(1));
                currentText = new StringBuilder(line);
                System.out.println("→ Found new requirement ID: " + currentReq.getId());
            } else if (currentReq != null && !line.isEmpty()) {
                // Continue building current requirement text
                currentText.append(" ").append(line);
            }
        }

        // Add last requirement
        if (currentReq != null) {
            String fullText = currentText.toString().trim();
            currentReq.setText(fullText);
            currentReq.setKeywords(extractKeywords(fullText));
            currentReq.setStatus("PENDING");
            requirements.add(currentReq);
            System.out.println("✓ Added last requirement: " + currentReq.getId());
        }

        System.out.println("========================================");
        System.out.println("RESULT: Parsed " + requirements.size() + " requirements");
        for (Requirement req : requirements) {
            System.out.println("  - " + req.getId() + ": " + req.getText());
            System.out.println("    Keywords: " + req.getKeywords());
        }
        System.out.println("========================================");

        return requirements;
    }

    /**
     * Extracts important keywords from requirement text
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();

        // Convert to lowercase for processing
        String lowerText = text.toLowerCase();

        // Remove common words
        Set<String> stopWords = Set.of(
                "the", "shall", "be", "a", "an", "is", "of", "to", "and", "or",
                "for", "in", "on", "with", "that", "this", "should", "must", "will"
        );

        // Split and filter
        String[] words = lowerText.split("\\W+");
        for (String word : words) {
            if (!stopWords.contains(word) && word.length() > 2) {
                keywords.add(word);
            }
        }

        return keywords;
    }
}