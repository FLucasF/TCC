package com.loja.checkout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.ResumoResponse;
import com.loja.checkout.model.ErroResponse;
import com.loja.checkout.service.CheckoutService;
import com.loja.checkout.service.ErroCheckoutException;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody PedidoRequest pedido) {
        try {
            ResumoResponse resumo = checkoutService.calcularResumo(pedido);
            return ResponseEntity.ok(resumo);
        } catch (ErroCheckoutException e) {
            ErroResponse erro = new ErroResponse(e.getCodigo());
            return ResponseEntity.badRequest().body(erro);
        }
    }

}
