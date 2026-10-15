package com.loja.resumo.web;

import com.loja.resumo.dto.ResumoRequest;
import com.loja.resumo.dto.ResumoResponse;
import com.loja.resumo.servico.CalculadoraResumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class ResumoController {

    private final CalculadoraResumo calculadoraResumo;

    public ResumoController(CalculadoraResumo calculadoraResumo) {
        this.calculadoraResumo = calculadoraResumo;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return calculadoraResumo.calcular(request);
    }
}
