package com.loja.checkout.controller;

import com.loja.checkout.dto.*;
import com.loja.checkout.servico.CheckoutService;
import com.loja.checkout.servico.ErroCheckoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        } catch (ErroCheckoutException e) {
            return ResponseEntity.badRequest()
                    .body(new ErroResponse(e.getCodigoErro()));
        }
    }
}
