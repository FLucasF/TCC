package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Entrega no mesmo dia por motoboy, so para pedidos de ate 5 kg. */
public final class EntregaMotoboy implements Entrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final int PRAZO_DIAS = 0;

    @Override
    public Frete calcular(BigDecimal pesoKg) {
        return new Frete(Dinheiro.emCentavos(VALOR), PRAZO_DIAS);
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
