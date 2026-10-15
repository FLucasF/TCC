package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 25,00 + R$ 4,50 por kg do pedido, em 2 dias. */
@Component
public class Expressa implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return FIXO.add(POR_KG.multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
