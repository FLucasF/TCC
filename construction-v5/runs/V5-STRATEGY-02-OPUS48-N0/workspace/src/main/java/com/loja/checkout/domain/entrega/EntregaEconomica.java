package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg do pedido, prazo de 7 dias. */
@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public boolean atende(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BASE.add(POR_KG.multiply(pesoTotalKg));
    }
}
