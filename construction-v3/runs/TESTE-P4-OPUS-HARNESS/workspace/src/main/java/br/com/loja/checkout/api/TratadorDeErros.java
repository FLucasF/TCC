package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(ErroCheckout.class)
    ResponseEntity<ErroResponse> erroDeCheckout(ErroCheckout erro) {
        return respostaDe(erro.codigo());
    }

    /** Corpo que o site nao conseguiu montar direito conta como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException erro) {
        return respostaDe(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> respostaDe(CodigoErro codigo) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(codigo.name()));
    }
}
