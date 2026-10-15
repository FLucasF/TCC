package br.com.loja.checkout.api;

import br.com.loja.checkout.Erro;
import br.com.loja.checkout.PedidoRecusadoException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TratamentoErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    Map<String, String> recusado(PedidoRecusadoException e) {
        return Map.of("erro", e.erro().name());
    }

    /** JSON que nem dá para ler (ex.: quantidade em texto) é tratado como pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> ilegivel(HttpMessageNotReadableException e) {
        return Map.of("erro", Erro.PEDIDO_INVALIDO.name());
    }
}
