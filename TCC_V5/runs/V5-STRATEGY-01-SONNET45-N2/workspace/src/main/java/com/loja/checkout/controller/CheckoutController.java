package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody PedidoRequest pedido) {
        try {
            ResumoResponse resumo = checkoutService.calcularResumo(pedido);
            return ResponseEntity.ok(resumo);
        } catch (CheckoutException e) {
            ErroResponse erro = new ErroResponse(e.getCodigo().name());
            return ResponseEntity.badRequest().body(erro);
        }
    }
}
