package com.loja.checkout.controller;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.ErroResponse;
import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.Resumo;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<Resumo> calcularResumo(@RequestBody PedidoRequest pedido) {
        Resumo resumo = checkoutService.calcularResumo(pedido);
        return ResponseEntity.ok(resumo);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> handleCheckoutException(CheckoutException ex) {
        ErroResponse erro = new ErroResponse(ex.getCodigo());
        return ResponseEntity.badRequest().body(erro);
    }
}
