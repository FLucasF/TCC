package br.com.loja.checkout.resumo;

import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
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
    Map<String, String> pedidoRecusado(PedidoRecusadoException e) {
        return Map.of("erro", e.codigo().name());
    }

    /** Corpo que não dá para ler (JSON malformado, número em formato errado) é um pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> corpoIlegivel(HttpMessageNotReadableException e) {
        return Map.of("erro", CodigoErro.PEDIDO_INVALIDO.name());
    }
}
