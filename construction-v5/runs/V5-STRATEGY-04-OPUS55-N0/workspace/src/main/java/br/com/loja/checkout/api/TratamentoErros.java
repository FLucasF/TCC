package br.com.loja.checkout.api;

import br.com.loja.checkout.CheckoutException;
import br.com.loja.checkout.Erros;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratamentoErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(CheckoutException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.codigo()));
    }

    /** Corpo ausente ou JSON mal formado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(Erros.PEDIDO_INVALIDO));
    }
}
