package com.loja.checkout.api;

import com.loja.checkout.api.dto.ErroResponse;
import com.loja.checkout.domain.ErroPedido;
import com.loja.checkout.domain.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz as recusas para o corpo { "erro": "CODIGO" }. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusadoException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.erro().name()));
    }

    /** JSON malformado ou com tipo errado em algum campo: o pedido nao da para ler. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(ErroPedido.PEDIDO_INVALIDO.name()));
    }
}
