package com.loja.pedidos.controller;

import com.loja.pedidos.dto.ErrorResponseDTO;
import com.loja.pedidos.exception.PedidoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PedidoException.class)
    public ResponseEntity<ErrorResponseDTO> handlePedidoException(PedidoException e) {
        ErrorResponseDTO error = new ErrorResponseDTO(e.getErrorCode());
        return new ResponseEntity<>(error, e.getStatusCode());
    }
}
