package com.loja.checkout;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CheckoutController {

    private final ResumoService resumoService;

    public CheckoutController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse calcularResumo(@RequestBody ResumoRequest request) {
        return resumoService.calcular(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> tratarErro(CheckoutException e) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("erro", e.getCodigo()));
    }
}
