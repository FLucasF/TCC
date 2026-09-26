package com.loja.roupas.api;

import com.loja.roupas.domain.CheckoutException;
import com.loja.roupas.domain.CheckoutService;
import com.loja.roupas.dto.ErroResponse;
import com.loja.roupas.dto.ResumoCheckoutRequest;
import com.loja.roupas.dto.ResumoCheckoutResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController() {
        this.checkoutService = new CheckoutService();
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody ResumoCheckoutRequest request) {
        try {
            ResumoCheckoutResponse response = checkoutService.calcular(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            return ResponseEntity.badRequest().body(new ErroResponse(e.getCodigo()));
        }
    }
}
