package com.loja.checkout.delivery;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivelPara(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoKg));
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }
}
