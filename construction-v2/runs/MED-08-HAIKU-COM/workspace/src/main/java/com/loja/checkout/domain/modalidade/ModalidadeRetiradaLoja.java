package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class ModalidadeRetiradaLoja implements Modalidade {
    private static final int PRAZO = 1;

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int obterPrazoEntrega() {
        return PRAZO;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public String obterCodigo() {
        return "RETIRADA_LOJA";
    }
}
