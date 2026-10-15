package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Cobra um valor fixo mais um tanto por quilo do pedido. */
public record TarifaPorPeso(BigDecimal fixo, BigDecimal porKg, int prazoDias) implements Entrega {

    @Override
    public Frete calcular(BigDecimal pesoKg) {
        return new Frete(Dinheiro.emCentavos(fixo.add(porKg.multiply(pesoKg))), prazoDias);
    }
}
