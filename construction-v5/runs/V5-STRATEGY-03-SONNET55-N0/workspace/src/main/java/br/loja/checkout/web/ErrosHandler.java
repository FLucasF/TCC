package br.loja.checkout.web;

import br.loja.checkout.CheckoutException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ErrosHandler {

    @ExceptionHandler(CheckoutException.class)
    ResponseEntity<Map<String, String>> recusado(CheckoutException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erro", e.codigo()));
    }

    /** JSON ilegível, vazio ou com tipos errados. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> ilegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erro", "PEDIDO_INVALIDO"));
    }
}
