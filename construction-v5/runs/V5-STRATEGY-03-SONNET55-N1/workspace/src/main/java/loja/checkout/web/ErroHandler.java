package loja.checkout.web;

import loja.checkout.dominio.CodigoErro;
import loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(PedidoRecusadoException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroResponse(e.codigo().name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
