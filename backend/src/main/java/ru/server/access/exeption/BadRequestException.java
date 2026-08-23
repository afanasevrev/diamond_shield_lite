package ru.server.access.exeption;


public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}