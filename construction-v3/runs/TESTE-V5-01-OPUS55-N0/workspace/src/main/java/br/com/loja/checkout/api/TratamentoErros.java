package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratamentoErros {

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse pedidoRecusado(CheckoutException e) {
        return new ErroResponse(e.getCodigo().name());
    }

    /** Corpo ausente ou em formato que não dá para ler (ex.: texto no lugar de número). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse corpoIlegivel(HttpMessageNotReadableException e) {
        return new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name());
    }
}
