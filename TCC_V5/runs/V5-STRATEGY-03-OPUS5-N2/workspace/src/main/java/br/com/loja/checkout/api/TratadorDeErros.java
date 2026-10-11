package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Pedido recusado volta so com o codigo do problema. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return recusar(excecao.codigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return recusar(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusar(CodigoErro codigo) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(codigo.name()));
    }
}
