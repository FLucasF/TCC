package com.loja.resumo.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EconomicaEntrega implements ModalidadeEntrega {

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
    public boolean disponivelPara(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return BASE.add(POR_KG.multiply(pesoKg));
    }
}
