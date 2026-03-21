package com.trishal.journalApp.exception;

import com.trishal.journalApp.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JournalAppException.class)
    public ResponseEntity<ApiResponse<Void>> handleJournalAppException(
            JournalAppException ex, HttpServletRequest request) {

        log.warn("JournalAppException [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());

        ApiResponse.ApiError error = ApiResponse.ApiError.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(
                ApiResponse.error(ex.getErrorCode().getMessage(), List.of(error)),
                ex.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiResponse.ApiError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ApiResponse.ApiError.builder()
                        .code(ErrorCode.VALIDATION_FAILED.getCode())
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.VALIDATION_FAILED.getMessage(), fieldErrors),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        ApiResponse.ApiError error = ApiResponse.ApiError.builder()
                .code(ErrorCode.ACCESS_DENIED.getCode())
                .message(ErrorCode.ACCESS_DENIED.getMessage())
                .build();

        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.ACCESS_DENIED.getMessage(), List.of(error)),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {

        ApiResponse.ApiError error = ApiResponse.ApiError.builder()
                .code(ErrorCode.USER_INVALID_CREDENTIALS.getCode())
                .message(ErrorCode.USER_INVALID_CREDENTIALS.getMessage())
                .build();

        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.USER_INVALID_CREDENTIALS.getMessage(), List.of(error)),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request body at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiResponse.ApiError error = ApiResponse.ApiError.builder()
                .code(ErrorCode.INVALID_REQUEST_BODY.getCode())
                .message(ErrorCode.INVALID_REQUEST_BODY.getMessage())
                .build();

        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.INVALID_REQUEST_BODY.getMessage(), List.of(error)),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);

        ApiResponse.ApiError error = ApiResponse.ApiError.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .build();

        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), List.of(error)),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
