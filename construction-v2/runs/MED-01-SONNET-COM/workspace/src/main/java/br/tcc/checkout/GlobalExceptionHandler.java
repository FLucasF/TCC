package br.tcc.checkout;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.tcc.checkout.dto.ErroResponse;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErroResponse tratarCheckoutException(CheckoutException ex) {
        return new ErroResponse(ex.getCodigo().name());
    }
}
