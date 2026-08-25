package ru.server.access.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class, MethodArgumentNotValidException.class})
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
    /**
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internal(Exception exception) {
        exception.printStackTrace();
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
    }
     **/
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internal(Exception ex, HttpServletResponse response) {
        String contentType = response.getContentType();
        if (response.isCommitted()
                || (contentType != null && contentType.startsWith(MediaType.TEXT_EVENT_STREAM_VALUE))) {
            log.warn("Ошибка в streaming-ответе, тело не пишем: {}", ex.toString());
            return null;
        }
        log.error("Внутренняя ошибка", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApiError(1,
                        "",
                        OffsetDateTime.now().toString()));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientGone(AsyncRequestNotUsableException ex) {
        log.debug("SSE-клиент отключился: {}", ex.getMessage());
        // возврат void -> Spring не пишет тело, вторичной ошибки нет
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