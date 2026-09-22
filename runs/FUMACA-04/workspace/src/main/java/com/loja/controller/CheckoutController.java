package com.loja.controller;

import com.loja.dto.CheckoutRequestDTO;
import com.loja.dto.CheckoutResponseDTO;
import com.loja.dto.ErroResponseDTO;
import com.loja.service.CheckoutService;
import com.loja.service.ErroCheckout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequestDTO request) {
        try {
            CheckoutResponseDTO response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (ErroCheckout e) {
            ErroResponseDTO erro = new ErroResponseDTO(e.getCodigo());
            return ResponseEntity.badRequest().body(erro);
        }
    }

}
