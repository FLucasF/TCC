package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal VALOR_FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5.00");
    private static final int PRAZO_DIAS = 0;

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularValor(BigDecimal pesoTotalKg) {
        return VALOR_FIXO;
    }

    @Override
    public int obterPrazoDias() {
        return PRAZO_DIAS;
    }
}
