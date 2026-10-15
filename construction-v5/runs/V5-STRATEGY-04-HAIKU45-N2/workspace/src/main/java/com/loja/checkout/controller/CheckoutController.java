package com.loja.checkout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.loja.checkout.model.RessumoRequest;
import com.loja.checkout.service.RessumoService;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    @Autowired
    private RessumoService ressumoService;

    @PostMapping("/resumo")
    public ResponseEntity<Object> resumo(@RequestBody RessumoRequest request) {
        Object resultado = ressumoService.calcular(request);
        return ResponseEntity.ok(resultado);
    }
}
