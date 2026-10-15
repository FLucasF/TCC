package br.com.loja.checkout.api;

import br.com.loja.checkout.calculo.CodigoErro;
import br.com.loja.checkout.calculo.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratamentoErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponse pedidoRecusado(PedidoRecusadoException e) {
        return new ErroResponse(e.getCodigo().name());
    }

    /** Corpo ausente, JSON malformado ou campos com tipo incompatível. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse corpoIlegivel(HttpMessageNotReadableException e) {
        return new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name());
    }
}
