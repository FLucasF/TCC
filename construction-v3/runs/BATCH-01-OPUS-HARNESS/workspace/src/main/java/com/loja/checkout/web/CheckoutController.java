package com.loja.checkout.web;

import com.loja.checkout.CalculadoraResumo;
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
    public ResumoResponse resumo(@RequestBody ResumoRequest requisicao) {
        return calculadora.calcular(requisicao);
    }

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<ErroResponse> erroDeCheckout(ErroCheckout erro) {
        return ResponseEntity.badRequest().body(new ErroResponse(erro.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
