package com.loja.checkout.web;

import com.loja.checkout.dominio.PedidoRecusadoException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Quando o pedido e recusado, o servico devolve so o codigo do problema no
 * formato { "erro": "CODIGO" }.
 */
@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<Map<String, String>> recusado(PedidoRecusadoException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("erro", ex.getCodigo().name()));
    }
}
