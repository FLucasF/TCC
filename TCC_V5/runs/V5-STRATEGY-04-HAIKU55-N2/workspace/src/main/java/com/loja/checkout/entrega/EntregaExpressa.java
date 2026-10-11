package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaExpressa implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public int prazoDias() {
        return 2;
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
    }
}
