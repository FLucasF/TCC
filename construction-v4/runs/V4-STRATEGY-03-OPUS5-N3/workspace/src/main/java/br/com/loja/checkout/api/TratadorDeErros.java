package br.com.loja.checkout.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.loja.checkout.dominio.PedidoRecusadoException;

/** Traduz pedido recusado para a resposta de erro do site. */
@RestControllerAdvice
public class TratadorDeErros {

    private static final String PEDIDO_INVALIDO = "PEDIDO_INVALIDO";

    @ExceptionHandler(PedidoRecusadoException.class)
    ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return erro(excecao.codigo());
    }

    /** Corpo que nem da para ler (numero onde era texto, JSON quebrado). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return erro(PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> erro(String codigo) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(codigo));
    }
}
