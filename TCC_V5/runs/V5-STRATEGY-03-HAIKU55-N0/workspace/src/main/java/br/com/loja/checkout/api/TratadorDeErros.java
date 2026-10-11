package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> erroDeNegocio(CheckoutException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(e.codigo().name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
