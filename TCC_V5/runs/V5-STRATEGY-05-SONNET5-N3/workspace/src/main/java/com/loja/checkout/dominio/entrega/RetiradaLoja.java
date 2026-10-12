package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements OpcaoEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal custo(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
