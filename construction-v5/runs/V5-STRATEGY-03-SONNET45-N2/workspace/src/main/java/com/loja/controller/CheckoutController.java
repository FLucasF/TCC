package com.loja.controller;

import com.loja.domain.Resumo;
import com.loja.dto.PedidoRequest;
import com.loja.dto.ResumoResponse;
import com.loja.service.CalculadoraResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumoService calculadora;

    public CheckoutController(CalculadoraResumoService calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResponseEntity<ResumoResponse> calcularResumo(@RequestBody PedidoRequest request) {
        Resumo resumo = calculadora.calcular(request);

        ResumoResponse response = new ResumoResponse(
            resumo.subtotalProdutos(),
            resumo.descontoCupom(),
            resumo.frete(),
            resumo.prazoEntregaDias(),
            resumo.seguro(),
            resumo.ajustePagamento(),
            resumo.totalFinal(),
            resumo.parcelas(),
            resumo.valorParcela(),
            resumo.creditoProximaCompra(),
            resumo.brinde()
        );

        return ResponseEntity.ok(response);
    }
}
