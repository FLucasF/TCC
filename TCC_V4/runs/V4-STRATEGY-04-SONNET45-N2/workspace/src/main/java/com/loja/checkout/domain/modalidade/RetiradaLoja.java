package com.loja.checkout.domain.modalidade;

import com.loja.checkout.domain.ModalidadeEntrega;
import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("0.00");
    }

    @Override
    public int obterPrazoDias() {
        return 1;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotalKg) {
        return true;
    }
}
