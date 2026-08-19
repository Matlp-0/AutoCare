package com.example.myapplication.domain.document;

public class AnalysisException extends Exception {

    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
