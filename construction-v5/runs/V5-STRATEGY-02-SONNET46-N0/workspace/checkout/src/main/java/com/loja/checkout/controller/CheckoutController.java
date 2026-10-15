package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroDto;
import com.loja.checkout.dto.PedidoDto;
import com.loja.checkout.dto.ResumoDto;
import com.loja.checkout.model.CheckoutException;
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
    public ResponseEntity<?> resumo(@RequestBody PedidoDto pedido) {
        try {
            ResumoDto resumo = checkoutService.calcular(pedido);
            return ResponseEntity.ok(resumo);
        } catch (CheckoutException e) {
            return ResponseEntity.unprocessableEntity().body(new ErroDto(e.getCodigo()));
        }
    }
}
