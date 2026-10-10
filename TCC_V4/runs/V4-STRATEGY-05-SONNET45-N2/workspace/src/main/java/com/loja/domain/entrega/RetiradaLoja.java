package com.loja.domain.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public boolean aceita(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public ResultadoFrete calcularFrete(BigDecimal pesoTotal) {
        return new ResultadoFrete(new BigDecimal("0.00"), 1);
    }
}
