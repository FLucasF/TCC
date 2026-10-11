package com.loja.checkout.web;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ErroPedido;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public CheckoutResponse resumo(@RequestBody CheckoutRequest req) {
        return calculadora.calcular(req);
    }

    @ExceptionHandler(ErroPedido.class)
    @ResponseStatus(HttpStatus.OK)
    public ErroResponse erroPedido(ErroPedido e) {
        return new ErroResponse(e.codigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErroResponse erroParse(HttpMessageNotReadableException e) {
        return new ErroResponse("PEDIDO_INVALIDO");
    }
}
