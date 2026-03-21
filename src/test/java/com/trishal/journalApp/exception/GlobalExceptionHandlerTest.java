package com.trishal.journalApp.exception;

import com.trishal.journalApp.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void handleJournalAppException_shouldReturnCorrectStatusAndBody() {
        JournalAppException ex = new JournalAppException(ErrorCode.USER_NOT_FOUND, "username: ghost");

        ResponseEntity<ApiResponse<Void>> response = handler.handleJournalAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo("ERR_1001");
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo("ERR_5003");
    }

    @Test
    void handleAuthentication_shouldReturn401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthentication(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo("ERR_1006");
    }

    @Test
    void handleGeneric_shouldReturn500() {
        when(request.getRequestURI()).thenReturn("/journal");
        RuntimeException ex = new RuntimeException("Unexpected");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo("ERR_9001");
    }

    @Test
    void handleJournalAppException_shouldMapAllErrorCodes() {
        for (ErrorCode code : new ErrorCode[]{
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.JOURNAL_ENTRY_NOT_FOUND,
                ErrorCode.GEMINI_SERVICE_UNAVAILABLE,
                ErrorCode.KAFKA_PUBLISH_FAILED
        }) {
            JournalAppException ex = new JournalAppException(code);
            ResponseEntity<ApiResponse<Void>> response = handler.handleJournalAppException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(code.getHttpStatus());
            assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo(code.getCode());
        }
    }

    @Test
    void handleMessageNotReadable_shouldReturn400() {
        when(request.getRequestURI()).thenReturn("/public/signup");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrors().get(0).getCode()).isEqualTo("ERR_6002");
    }
}
