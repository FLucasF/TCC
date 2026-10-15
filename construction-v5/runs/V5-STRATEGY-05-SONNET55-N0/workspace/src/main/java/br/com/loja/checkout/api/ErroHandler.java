package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<Map<String, String>> recusado(PedidoRecusadoException e) {
        return resposta(e.codigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> ilegivel(HttpMessageNotReadableException e) {
        return resposta(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<Map<String, String>> resposta(CodigoErro codigo) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erro", codigo.name()));
    }
}
