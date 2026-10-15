package br.com.loja.checkout.api;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.ErroPedidoException;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroPedidoException.class)
    public ResponseEntity<Map<String, String>> erroDePedido(ErroPedidoException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getCodigo().name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido() {
        return ResponseEntity.badRequest().body(Map.of("erro", CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
