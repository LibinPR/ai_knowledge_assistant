package com.yourorg.aiknowledgeassistant.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //Email already exists handler
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<?> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 409,
                        "error", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 400,
                        "error", ex.getMessage()
                ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), null));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<?> handleUpstreamResponse(RestClientResponseException ex) {
        log.warn("Upstream service returned {}", ex.getStatusCode().value());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(errorResponse(HttpStatus.BAD_GATEWAY,
                        "An AI provider rejected the request. Please retry shortly.", null));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<?> handleUpstreamUnavailable(ResourceAccessException ex) {
        log.warn("Upstream service is unavailable", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                        "An AI provider is temporarily unavailable. Please retry shortly.", null));
    }

    //Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String , String> errors = new HashMap<>();

        for(FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField() , error.getDefaultMessage());
        }

        return ResponseEntity
                .badRequest()
                .body(errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation Failed",
                        errors
                ));
    }

    //Entity Not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage(),
                        null
                ));
    }

    //Access Denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorResponse(
                        HttpStatus.FORBIDDEN,
                        "Access Denied",
                        null
                ));
    }

    //Generic Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        log.error("Unhandled Exception" , ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Something went wrong",
                        null
                ));
    }

    //max file size handler
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(errorResponse(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "File size exceeds limit[10MB]",
                        null
                ));
    }

    private Map<String , Object> errorResponse(
            HttpStatus status,
            String message,
            Object details
    ) {
        Map<String , Object> body = new HashMap<>();
        body.put("timestamp" , Instant.now());
        body.put("status" , status.value());
        body.put("error" , message);
        body.put("details" , details);
        return body;
    }
}
