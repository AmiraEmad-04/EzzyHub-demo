package com.ezzyhub.service;

import com.ezzyhub.model.ImportIssue;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Service
public class ImportChecker {

    private final JavaParser javaParser;

    // Common Java built-in classes that don't need imports
    private static final Set<String> BUILT_IN_TYPES = Set.of(
            "String", "Integer", "Double", "Boolean", "Long", "Float",
            "Character", "Byte", "Short", "Object", "System", "Math"
    );

    public ImportChecker() {
        this.javaParser = new JavaParser();
    }

    /**
     * Checks for missing imports in Java files
     */
    public List<ImportIssue> checkImports(List<File> javaFiles) {
        System.out.println("Checking imports in " + javaFiles.size() + " files...");
        List<ImportIssue> issues = new ArrayList<>();

        for (File file : javaFiles) {
            try {
                CompilationUnit cu = javaParser.parse(new FileInputStream(file))
                        .getResult()
                        .orElse(null);

                if (cu != null) {
                    issues.addAll(checkFileImports(cu, file.getName()));
                }
            } catch (Exception e) {
                System.err.println("Error checking imports in: " + file.getName());
            }
        }

        System.out.println("Found " + issues.size() + " import issues");
        return issues;
    }

    /**
     * Checks imports for a single file
     */
    private List<ImportIssue> checkFileImports(CompilationUnit cu, String fileName) {
        List<ImportIssue> issues = new ArrayList<>();

        // Get all imports
        Set<String> importedClasses = new HashSet<>();
        for (ImportDeclaration imp : cu.getImports()) {
            String importName = imp.getNameAsString();
            // Extract class name from full import
            String className = importName.substring(importName.lastIndexOf('.') + 1);
            importedClasses.add(className);
        }

        // Get declared classes in this file
        Set<String> declaredClasses = new HashSet<>();
        cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .forEach(cls -> declaredClasses.add(cls.getNameAsString()));

        // Find all class usages
        Set<String> usedClasses = new HashSet<>();

        // Check object creations (new ClassName())
        cu.findAll(ObjectCreationExpr.class).forEach(obj -> {
            String className = obj.getType().getNameAsString();
            usedClasses.add(className);
        });

        // Check type references
        cu.findAll(ClassOrInterfaceType.class).forEach(type -> {
            String className = type.getNameAsString();
            usedClasses.add(className);
        });

        // Check for missing imports
        for (String usedClass : usedClasses) {
            // Skip if it's a built-in type
            if (BUILT_IN_TYPES.contains(usedClass)) {
                continue;
            }

            // Skip if it's declared in this file
            if (declaredClasses.contains(usedClass)) {
                continue;
            }

            // Skip generic type parameters (T, E, K, V, etc.)
            if (usedClass.length() == 1 && Character.isUpperCase(usedClass.charAt(0))) {
                continue;
            }

            // Check if imported
            if (!importedClasses.contains(usedClass)) {
                ImportIssue issue = new ImportIssue();
                issue.setClassName(fileName);
                issue.setMissingImport(usedClass);
                issue.setLineNumber(0);
                issues.add(issue);
                System.out.println("Missing import: " + usedClass + " in " + fileName);
            }
        }

        return issues;
    }
}