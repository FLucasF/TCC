package com.loja.checkout.controller;

import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.exception.ErroCheckout;
import com.loja.checkout.service.ResumoCompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private ResumoCompraService resumoCompraService;

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody ResumoRequest request) {
        try {
            ResumoResponse response = resumoCompraService.calcular(request);
            return ResponseEntity.ok(response);
        } catch (ErroCheckout e) {
            return ResponseEntity.badRequest().body(new ErroResponse(e.getCodigo()));
        }
    }
}
