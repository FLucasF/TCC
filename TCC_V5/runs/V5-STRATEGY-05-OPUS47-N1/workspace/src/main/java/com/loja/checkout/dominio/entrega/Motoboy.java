package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal LIMITE_PESO = new BigDecimal("5");

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(LIMITE_PESO) <= 0;
    }

    @Override
    public Frete calcular(BigDecimal pesoKg) {
        return new Frete(Dinheiro.arredonda(new BigDecimal("18.00")), 0);
    }
}
