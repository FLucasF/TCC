package com.loja.checkout.web;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.PedidoRequisicao;
import com.loja.checkout.dominio.Resumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public Resumo resumo(@RequestBody(required = false) PedidoRequisicao requisicao) {
        return calculadora.calcular(requisicao == null
                ? new PedidoRequisicao(null, null, null, null, null)
                : requisicao);
    }
}
