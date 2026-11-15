package com.university.management.exception;

/** Thrown when an id does not exist -> 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
