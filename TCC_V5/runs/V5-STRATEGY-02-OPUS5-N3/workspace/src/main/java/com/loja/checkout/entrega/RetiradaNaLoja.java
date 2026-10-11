package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaNaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public Frete calcular(BigDecimal pesoKg) {
        return new Frete(BigDecimal.ZERO, 1);
    }
}
