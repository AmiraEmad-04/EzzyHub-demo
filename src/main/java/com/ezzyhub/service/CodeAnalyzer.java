package com.ezzyhub.service;

import com.ezzyhub.model.CodeFeature;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class CodeAnalyzer {

    private final JavaParser javaParser;

    public CodeAnalyzer() {
        this.javaParser = new JavaParser();
    }

    /**
     * Analyzes Java files and extracts code features
     */
    public List<CodeFeature> analyze(List<File> javaFiles) {
        System.out.println("========================================");
        System.out.println("Analyzing " + javaFiles.size() + " Java files...");
        List<CodeFeature> features = new ArrayList<>();

        for (File file : javaFiles) {
            try {
                System.out.println("Parsing: " + file.getName());
                CompilationUnit cu = javaParser.parse(new FileInputStream(file))
                        .getResult()
                        .orElse(null);

                if (cu != null) {
                    List<CodeFeature> fileFeatures = extractFeatures(cu);
                    features.addAll(fileFeatures);
                    System.out.println("  → Extracted " + fileFeatures.size() + " features from " + file.getName());
                }
            } catch (Exception e) {
                System.err.println("Error parsing file: " + file.getName());
                e.printStackTrace();
            }
        }

        System.out.println("========================================");
        System.out.println("TOTAL: Extracted " + features.size() + " code features");
        for (CodeFeature feature : features) {
            System.out.println("  - " + feature.getClassName() + "." + feature.getMethodName());
            System.out.println("    Keywords: " + feature.getKeywords());
        }
        System.out.println("========================================");

        return features;
    }

    /**
     * Extracts features from a compilation unit
     */
    private List<CodeFeature> extractFeatures(CompilationUnit cu) {
        List<CodeFeature> features = new ArrayList<>();

        // Find all classes
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            String className = cls.getNameAsString();

            // Find all methods in the class
            cls.findAll(MethodDeclaration.class).forEach(method -> {
                CodeFeature feature = new CodeFeature();
                feature.setClassName(className);
                feature.setMethodName(method.getNameAsString());
                feature.setType("METHOD");

                // Build description
                StringBuilder desc = new StringBuilder();
                desc.append("Method ").append(method.getNameAsString());
                desc.append(" in class ").append(className);

                // Check if it's a main method
                if (method.getNameAsString().equals("main") && method.isStatic()) {
                    desc.append(" [MAIN METHOD]");
                }

                // Check for comments that might indicate requirements
                method.getComment().ifPresent(comment -> {
                    String commentText = comment.getContent();
                    if (commentText.contains("REQ-")) {
                        desc.append(" [Implements: ").append(extractReqIds(commentText)).append("]");
                    }
                });

                feature.setDescription(desc.toString());

                // Extract keywords from method name and content
                List<String> keywords = extractMethodKeywords(method);
                feature.setKeywords(keywords);

                features.add(feature);
            });
        });

        return features;
    }

    /**
     * Extracts keywords from method name and body
     */
    private List<String> extractMethodKeywords(MethodDeclaration method) {
        List<String> keywords = new ArrayList<>();

        // Add method name as keywords
        String methodName = method.getNameAsString();
        keywords.add(methodName.toLowerCase());

        // Special handling for main method
        if (methodName.equals("main") && method.isStatic()) {
            keywords.add("main");
            keywords.add("entry");
            keywords.add("start");
            keywords.add("program");
            keywords.add("system");
        }

        // Split camelCase method names
        String[] nameParts = methodName.split("(?=[A-Z])");
        for (String part : nameParts) {
            if (!part.isEmpty()) {
                keywords.add(part.toLowerCase());
            }
        }

        // Add parameter types
        method.getParameters().forEach(param -> {
            String paramType = param.getType().asString().toLowerCase();
            keywords.add(paramType);

            // Extract simple type name if it's fully qualified
            if (paramType.contains(".")) {
                String simpleName = paramType.substring(paramType.lastIndexOf('.') + 1);
                keywords.add(simpleName);
            }
        });

        // Check method body for key operations
        String methodBody = method.getBody().map(Object::toString).orElse("");

        // Math operations
        if (methodBody.contains("+") || methodBody.contains("add")) keywords.add("add");
        if (methodBody.contains("-") || methodBody.contains("subtract")) keywords.add("subtract");
        if (methodBody.contains("*") || methodBody.contains("multiply")) keywords.add("multiply");
        if (methodBody.contains("/") || methodBody.contains("divide")) keywords.add("divide");

        // Special conditions
        if (methodBody.contains("== 0") || methodBody.contains("!= 0")) keywords.add("zero");
        if (methodBody.contains("throw") || methodBody.contains("Exception")) keywords.add("error");

        // Input/Output
        if (methodBody.contains("Scanner") || methodBody.contains("input")) keywords.add("input");
        if (methodBody.contains("System.out") || methodBody.contains("print")) keywords.add("output");

        // Validation
        if (methodBody.contains("validate") || methodBody.contains("check")) keywords.add("validate");
        if (methodBody.contains("if (") || methodBody.contains("else")) keywords.add("condition");

        return keywords;
    }

    /**
     * Extracts requirement IDs from comments
     */
    private String extractReqIds(String comment) {
        StringBuilder reqIds = new StringBuilder();
        String[] words = comment.split("\\s+");
        for (String word : words) {
            if (word.startsWith("REQ-")) {
                reqIds.append(word.replaceAll("[^A-Z0-9-]", "")).append(" ");
            }
        }
        return reqIds.toString().trim();
    }
}