package com.ezzyhub.model;

import lombok.Data;

@Data
public class ImportIssue {
    private String className;
    private String missingImport;
    private int lineNumber;
}