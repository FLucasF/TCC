package com.example.api.exception;

import com.example.api.dto.ErrorDto;
import com.example.application.exception.ApplicationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorDto> handleApplicationException(ApplicationException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(new ErrorDto(ex.getErrorCode()));
    }
}
