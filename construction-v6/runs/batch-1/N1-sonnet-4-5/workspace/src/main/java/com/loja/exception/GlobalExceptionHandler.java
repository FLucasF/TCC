package com.loja.exception;

import com.loja.integration.TransLogUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException e) {
        return ResponseEntity
            .status(e.getHttpStatus())
            .body(Map.of("error", e.getErrorCode()));
    }

    @ExceptionHandler(TransLogUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleTransLogUnavailable(TransLogUnavailableException e) {
        return ResponseEntity
            .status(503)
            .body(Map.of("error", "CARRIER_UNAVAILABLE"));
    }
}
