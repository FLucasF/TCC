package com.loja.checkout.service.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EconomicaModalidade implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
