package com.university.management.exception;

import lombok.Getter;

/** Thrown when an email or roll number is already used -> 409. */
@Getter
public class DuplicateResourceException extends RuntimeException {

    // name of the form field that is duplicated ("email" or "rollNumber"), used by the web forms
    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }
}
