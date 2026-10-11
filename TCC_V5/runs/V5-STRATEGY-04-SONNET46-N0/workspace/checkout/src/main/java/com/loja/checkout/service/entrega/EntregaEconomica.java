package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;

@Component
public class EntregaEconomica implements ModalidadeEntrega {
    @Override
    public String getCodigo() { return "ECONOMICA"; }

    @Override
    public boolean aceita(double pesoTotalKg) { return true; }

    @Override
    public ResultadoFrete calcular(double pesoTotalKg) {
        return new ResultadoFrete(12.00 + 2.00 * pesoTotalKg, 7);
    }
}
