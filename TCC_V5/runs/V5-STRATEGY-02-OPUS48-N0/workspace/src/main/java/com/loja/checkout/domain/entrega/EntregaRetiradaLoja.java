package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Retirada na loja: gratis, prazo de 1 dia. */
@Component
public class EntregaRetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public boolean atende(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO;
    }
}
