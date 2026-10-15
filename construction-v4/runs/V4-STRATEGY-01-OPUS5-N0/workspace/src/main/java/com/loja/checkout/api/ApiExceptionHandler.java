package com.loja.checkout.api;

import com.loja.checkout.api.dto.ErroResponse;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Transforma a recusa do pedido na resposta {@code {"erro": "CODIGO"}}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> checkoutRecusado(CheckoutException excecao) {
        return recusar(excecao.getCodigo());
    }

    /** Corpo que o site mandou e ilegivel (ex.: quantidade que nao e numero). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return recusar(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusar(CodigoErro codigo) {
        return ResponseEntity.badRequest().body(new ErroResponse(codigo.name()));
    }
}
