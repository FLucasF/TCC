package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResposta;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.ResumoCheckout;
import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CheckoutService;
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
    public ResponseEntity<?> calcularResumo(@RequestBody RequisicaoCheckout requisicao) {
        try {
            ResumoCheckout resumo = checkoutService.calcularResumo(requisicao);
            return ResponseEntity.ok(resumo);
        } catch (CheckoutException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResposta(e.getCodigo()));
        }
    }
}
