package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaEconomica implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
    }
}
