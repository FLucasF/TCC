package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class ModalidadeMotoboy implements Modalidade {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final int PRAZO = 0;
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5.00");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA;
    }

    @Override
    public int obterPrazoEntrega() {
        return PRAZO;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public String obterCodigo() {
        return "MOTOBOY";
    }
}
