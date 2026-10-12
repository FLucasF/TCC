package com.loja.checkout.entrega;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(BigDecimal.ZERO);
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
