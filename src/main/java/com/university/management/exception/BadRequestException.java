package com.university.management.exception;

/** Thrown for invalid input that bean validation cannot catch -> 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
