package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.Erro;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ErroHandler {

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErroResponse recusado(CheckoutException e) {
        return new ErroResponse(e.erro().name());
    }

    /** JSON que não dá para ler (ex.: quantidade com texto) é tratado como pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErroResponse ilegivel(HttpMessageNotReadableException e) {
        return new ErroResponse(Erro.PEDIDO_INVALIDO.name());
    }
}
