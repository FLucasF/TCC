package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public Frete calcular(BigDecimal pesoKg) {
        BigDecimal valor = new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        return new Frete(Dinheiro.arredonda(valor), 7);
    }
}
