package com.loja.checkout.domain.modalidade;

import com.loja.checkout.domain.ModalidadeEntrega;
import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal LIMITE_PESO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public int obterPrazoDias() {
        return 0;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(LIMITE_PESO) <= 0;
    }
}
