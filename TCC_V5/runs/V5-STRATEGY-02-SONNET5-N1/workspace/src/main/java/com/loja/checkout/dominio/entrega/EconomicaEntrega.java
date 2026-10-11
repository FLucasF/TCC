package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EconomicaEntrega implements ModalidadeEntrega {

    private static final BigDecimal VALOR_BASE = new BigDecimal("12.00");
    private static final BigDecimal VALOR_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularValor(BigDecimal pesoTotalKg) {
        return VALOR_BASE.add(VALOR_POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
