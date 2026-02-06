package com.ezzyhub.service;

import com.ezzyhub.model.CodeFeature;
import com.ezzyhub.model.Requirement;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RequirementMatcher {

    /**
     * Matches requirements to code features
     */
    public void matchRequirements(List<Requirement> requirements, List<CodeFeature> features) {
        System.out.println("========================================");
        System.out.println("Matching " + requirements.size() + " requirements to " + features.size() + " features...");

        for (Requirement req : requirements) {
            MatchResult bestMatch = findBestMatch(req, features);

            req.setMatchScore(bestMatch.score);
            req.setMatchedFeature(bestMatch.featureName);

            // Set status based on match score
            if (bestMatch.score >= 0.6) {
                req.setStatus("IMPLEMENTED");
            } else if (bestMatch.score >= 0.3) {
                req.setStatus("PARTIAL");
            } else {
                req.setStatus("MISSING");
            }

            System.out.println("  " + req.getId() + " -> " + req.getStatus() +
                    " (score: " + String.format("%.2f", bestMatch.score) +
                    ", matched: " + bestMatch.featureName + ")");
        }
        System.out.println("========================================");
    }

    /**
     * Finds the best matching code feature for a requirement
     */
    private MatchResult findBestMatch(Requirement req, List<CodeFeature> features) {
        double bestScore = 0.0;
        String bestFeature = "No match found";

        System.out.println("\n  Matching " + req.getId() + " with keywords: " + req.getKeywords());

        for (CodeFeature feature : features) {
            double score = calculateSimilarity(req.getKeywords(), feature.getKeywords());

            // Boost score for operation-specific matches
            if (isOperationMatch(req, feature)) {
                score += 0.3;
                System.out.println("    + BOOST for " + feature.getMethodName() + " (operation match)");
            }

            // Boost for main method specifically
            if (req.getText().toLowerCase().contains("main method") &&
                    feature.getMethodName().equals("main")) {
                score += 0.4;
                System.out.println("    + BOOST for main method match");
            }

            System.out.println("    " + feature.getClassName() + "." + feature.getMethodName() +
                    " -> score: " + String.format("%.2f", score));

            if (score > bestScore) {
                bestScore = score;
                bestFeature = feature.getClassName() + "." + feature.getMethodName();
            }
        }

        System.out.println("  BEST: " + bestFeature + " (score: " + String.format("%.2f", bestScore) + ")");

        return new MatchResult(bestScore, bestFeature);
    }

    /**
     * Calculates keyword similarity using Jaccard index
     */
    private double calculateSimilarity(List<String> keywords1, List<String> keywords2) {
        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        Set<String> set1 = new HashSet<>(keywords1);
        Set<String> set2 = new HashSet<>(keywords2);

        // Intersection
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // Union
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        double similarity = (double) intersection.size() / union.size();

        // Bonus for exact word matches
        for (String k1 : keywords1) {
            if (keywords2.contains(k1) && k1.length() > 3) {
                similarity += 0.1;
            }
        }

        return Math.min(1.0, similarity);
    }

    /**
     * Checks if requirement matches operation type
     */
    private boolean isOperationMatch(Requirement req, CodeFeature feature) {
        String reqId = req.getId().toUpperCase();
        String reqText = req.getText().toLowerCase();
        String methodName = feature.getMethodName().toLowerCase();

        // Check for operation-specific patterns
        if ((reqId.contains("ADD") || reqText.contains("addition")) && methodName.contains("add")) return true;
        if ((reqId.contains("SUB") || reqText.contains("subtract")) && methodName.contains("subtract")) return true;
        if ((reqId.contains("MUL") || reqText.contains("multiply")) && methodName.contains("multiply")) return true;
        if ((reqId.contains("DIV") || reqText.contains("divide")) && methodName.contains("divide")) return true;
        if ((reqId.contains("INPUT") || reqText.contains("input")) && methodName.contains("validate")) return true;
        if ((reqId.contains("ERR") || reqText.contains("error")) && methodName.contains("error")) return true;
        if (reqText.contains("main method") && methodName.equals("main")) return true;

        return false;
    }

    /**
     * Generates suggestions for missing/partial requirements
     */
    public List<String> generateSuggestions(List<Requirement> requirements) {
        System.out.println("\nGenerating suggestions...");
        List<String> suggestions = new ArrayList<>();

        for (Requirement req : requirements) {
            if ("MISSING".equals(req.getStatus())) {
                suggestions.add(generateMissingSuggestion(req));
            } else if ("PARTIAL".equals(req.getStatus())) {
                suggestions.add(generatePartialSuggestion(req));
            }
        }

        System.out.println("Generated " + suggestions.size() + " suggestions");
        return suggestions;
    }

    private String generateMissingSuggestion(Requirement req) {
        return String.format(
                "Missing implementation for %s: %s. Consider adding methods or validation logic.",
                req.getId(),
                req.getText()
        );
    }

    private String generatePartialSuggestion(Requirement req) {
        return String.format(
                "Partial implementation for %s: %s. Review and complete the implementation in %s.",
                req.getId(),
                req.getText(),
                req.getMatchedFeature()
        );
    }

    // Helper class for match results
    private static class MatchResult {
        double score;
        String featureName;

        MatchResult(double score, String featureName) {
            this.score = score;
            this.featureName = featureName;
        }
    }
}