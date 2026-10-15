package com.loja.checkout;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController() {
        this.checkoutService = new CheckoutService();
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoRequest request) {
        try {
            ResumoResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (ErroCheckout e) {
            ErroResponse erro = new ErroResponse(e.getCodigo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
        }
    }
}
