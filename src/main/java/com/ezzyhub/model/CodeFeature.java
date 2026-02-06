package com.ezzyhub.model;

import lombok.Data;
import java.util.List;

@Data
public class CodeFeature {
    private String className;
    private String methodName;
    private String description;
    private List<String> keywords;
    private String type;  // METHOD, CLASS, FIELD
}