package br.com.loja.checkout.api;

import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.ErroCheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Toda falha de negocio vira 400 com { "erro": "CODIGO" }. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroCheckoutException.class)
    public ResponseEntity<ErroResposta> erroDeNegocio(ErroCheckoutException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResposta(excecao.codigo().name()));
    }

    /** Corpo ilegivel (JSON quebrado ou campo com tipo errado) conta como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResposta(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
