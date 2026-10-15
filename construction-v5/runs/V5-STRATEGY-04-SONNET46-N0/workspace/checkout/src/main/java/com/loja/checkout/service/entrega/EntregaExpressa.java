package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;

@Component
public class EntregaExpressa implements ModalidadeEntrega {
    @Override
    public String getCodigo() { return "EXPRESSA"; }

    @Override
    public boolean aceita(double pesoTotalKg) { return true; }

    @Override
    public ResultadoFrete calcular(double pesoTotalKg) {
        return new ResultadoFrete(25.00 + 4.50 * pesoTotalKg, 2);
    }
}
