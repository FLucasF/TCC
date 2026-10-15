package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public String getCodigo() { return "RETIRADA_LOJA"; }

    @Override
    public boolean aceita(double pesoTotalKg) { return true; }

    @Override
    public ResultadoFrete calcular(double pesoTotalKg) {
        return new ResultadoFrete(0.00, 1);
    }
}
