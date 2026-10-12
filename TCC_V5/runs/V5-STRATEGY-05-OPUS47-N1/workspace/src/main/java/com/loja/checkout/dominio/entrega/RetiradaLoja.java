package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public Frete calcular(BigDecimal pesoKg) {
        return new Frete(Dinheiro.arredonda(BigDecimal.ZERO), 1);
    }
}
