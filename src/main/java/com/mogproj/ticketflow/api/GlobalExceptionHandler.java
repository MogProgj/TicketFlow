package com.mogproj.ticketflow.api;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.mogproj.ticketflow.tickets.InvalidTicketStatusTransitionException;
import com.mogproj.ticketflow.tickets.TicketNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(TicketNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidTicketStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStatusTransition(
            InvalidTicketStatusTransitionException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed.", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, resolveBodyReadMessage(exception), Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        Class<?> requiredType = exception.getRequiredType();
        String message = "Invalid value for parameter '" + exception.getName() + "'.";

        if (requiredType != null && requiredType.isEnum()) {
            message = buildEnumMessage(exception.getName(), exception.getValue(), requiredType);
        }

        return buildResponse(HttpStatus.BAD_REQUEST, message, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
            Map<String, String> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String resolveBodyReadMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
                return "Unknown field '" + unrecognizedPropertyException.getPropertyName() + "'.";
            }
            if (cause instanceof InvalidFormatException invalidFormatException) {
                String fieldName = invalidFormatException.getPath().isEmpty()
                        ? "value"
                        : invalidFormatException.getPath().get(invalidFormatException.getPath().size() - 1)
                                .getFieldName();
                return buildEnumMessage(fieldName, invalidFormatException.getValue(),
                        invalidFormatException.getTargetType());
            }
            cause = cause.getCause();
        }

        return "Malformed request body.";
    }

    private String buildEnumMessage(String fieldName, Object rejectedValue, Class<?> enumType) {
        if (!enumType.isEnum()) {
            return "Invalid value for '" + fieldName + "'.";
        }

        String allowedValues = Arrays.stream(enumType.getEnumConstants())
                .map(value -> ((Enum<?>) value).name())
                .collect(Collectors.joining(", "));

        return "Invalid value '" + rejectedValue + "' for '" + fieldName + "'. Allowed values: " + allowedValues + ".";
    }
}
