package com.loja.checkout.controller;

import com.loja.checkout.dto.PedidoDto;
import com.loja.checkout.dto.ResumoDto;
import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoDto pedido) {
        try {
            ResumoDto resumo = checkoutService.calcular(pedido);
            return ResponseEntity.ok(resumo);
        } catch (CheckoutException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getCodigo()));
        }
    }
}
