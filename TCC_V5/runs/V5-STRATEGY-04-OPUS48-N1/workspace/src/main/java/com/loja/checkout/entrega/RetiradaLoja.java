package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
