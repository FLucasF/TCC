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
    public ErroResponse recusado(CheckoutException e) {
        return new ErroResponse(e.codigo().name());
    }

    /** JSON ausente, malformado ou com tipos errados (ex.: preço em texto). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse ilegivel(HttpMessageNotReadableException e) {
        return new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name());
    }
}
