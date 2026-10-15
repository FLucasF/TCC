package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements OpcaoEntrega {

    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(LIMITE_KG) <= 0;
    }

    @Override
    public BigDecimal custo(BigDecimal pesoTotalKg) {
        return CUSTO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
