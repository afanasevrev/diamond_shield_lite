package ru.server.access.exeption;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ApiError> badRequest(Exception exception) {
        String message = exception.getMessage();

        if (exception instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("Ошибка проверки данных");
        }

        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> conflict(
            IllegalStateException exception
    ) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internal(Exception exception) {
        exception.printStackTrace();
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера"
        );
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity.status(status).body(
                new ApiError(
                        status.value(),
                        message,
                        OffsetDateTime.now().toString()
                )
        );
    }

    public record ApiError(
            int status,
            String message,
            String timestamp
    ) {
    }
}