package ru.server.access.exeption;


public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}