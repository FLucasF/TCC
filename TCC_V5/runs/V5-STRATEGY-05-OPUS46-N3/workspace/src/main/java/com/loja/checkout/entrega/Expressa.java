package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class Expressa implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(pesoKg))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoDias() {
        return 2;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
