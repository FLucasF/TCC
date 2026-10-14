package br.com.loja.pedidos.comum;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(ErroNegocio.class)
    public ResponseEntity<Map<String, String>> negocio(ErroNegocio erro) {
        return ResponseEntity.status(erro.status()).body(Map.of("erro", erro.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoIlegivel(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("erro", "PEDIDO_INVALIDO"));
    }
}
