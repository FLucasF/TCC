package com.loja.checkout.api;

import com.loja.checkout.service.CheckoutService;
import com.loja.checkout.service.ErroCheckout;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> resumo(@RequestBody ResumoRequest request) {
        try {
            ResumoResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (ErroCheckout e) {
            return ResponseEntity.badRequest().body(new ErroResponse(e.getCodigo()));
        }
    }
}
