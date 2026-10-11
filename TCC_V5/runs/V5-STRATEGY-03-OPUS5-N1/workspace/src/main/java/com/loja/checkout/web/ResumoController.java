package com.loja.checkout.web;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.dominio.SolicitacaoResumo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody(required = false) SolicitacaoResumo solicitacao) {
        if (solicitacao == null) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return calculadora.calcular(solicitacao);
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResposta> pedidoRecusado(PedidoRecusadoException excecao) {
        return ResponseEntity.unprocessableEntity().body(new ErroResposta(excecao.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResposta(CodigoErro.PEDIDO_INVALIDO));
    }
}
