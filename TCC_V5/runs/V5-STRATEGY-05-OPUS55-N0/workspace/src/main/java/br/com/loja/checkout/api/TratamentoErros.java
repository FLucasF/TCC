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
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponse pedidoRecusado(CheckoutException e) {
        return new ErroResponse(e.codigo().name());
    }

    /** JSON malformado ou com tipos errados (ex.: texto no lugar do preço). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse jsonInvalido(HttpMessageNotReadableException e) {
        return new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name());
    }
}
