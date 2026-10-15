package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public RespostaResumo resumo(@RequestBody(required = false) RequisicaoResumo requisicao) {
        return calculadora.calcular(requisicao);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<RespostaErro> erroDeNegocio(CheckoutException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RespostaErro(excecao.getErro().name()));
    }

    /** Corpo ilegivel (numero em texto, JSON quebrado) tambem e pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RespostaErro(ErroCheckout.PEDIDO_INVALIDO.name()));
    }
}
