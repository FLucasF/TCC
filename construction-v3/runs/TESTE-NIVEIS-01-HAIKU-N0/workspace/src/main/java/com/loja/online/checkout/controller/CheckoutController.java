package com.loja.online.checkout.controller;

import com.loja.online.checkout.dto.ErroDTO;
import com.loja.online.checkout.dto.ResumoCheckoutRequestDTO;
import com.loja.online.checkout.dto.ResumoCheckoutResponseDTO;
import com.loja.online.checkout.exception.CheckoutException;
import com.loja.online.checkout.service.ResumoCheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoCheckoutService resumoCheckoutService;

    public CheckoutController(ResumoCheckoutService resumoCheckoutService) {
        this.resumoCheckoutService = resumoCheckoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> obterResumoCheckout(@RequestBody ResumoCheckoutRequestDTO request) {
        try {
            ResumoCheckoutResponseDTO resposta = resumoCheckoutService.calcularResumo(request);
            return ResponseEntity.ok(resposta);
        } catch (CheckoutException e) {
            return ResponseEntity.badRequest().body(new ErroDTO(e.getCodigo()));
        }
    }
}
