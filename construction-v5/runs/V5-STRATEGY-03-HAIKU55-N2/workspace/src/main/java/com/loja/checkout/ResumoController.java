package com.loja.checkout;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ResumoCalculator calculator;

    public ResumoController(ResumoCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest pedido) {
        return calculator.calcular(pedido);
    }
}
