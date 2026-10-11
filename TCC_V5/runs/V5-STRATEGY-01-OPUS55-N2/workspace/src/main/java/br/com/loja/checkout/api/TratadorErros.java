package br.com.loja.checkout.api;

import br.com.loja.checkout.resumo.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TratadorErros {

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErroResponse recusado(CheckoutException e) {
        return new ErroResponse(e.codigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErroResponse ilegivel(HttpMessageNotReadableException e) {
        return new ErroResponse("PEDIDO_INVALIDO");
    }
}
