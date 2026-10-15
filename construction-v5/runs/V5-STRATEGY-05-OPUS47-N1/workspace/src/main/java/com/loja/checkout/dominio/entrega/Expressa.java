package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {
    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public Frete calcular(BigDecimal pesoKg) {
        BigDecimal valor = new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        return new Frete(Dinheiro.arredonda(valor), 2);
    }
}
