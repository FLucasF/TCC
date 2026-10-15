package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Base das modalidades que cobram uma taxa fixa mais um valor por kg. */
abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal fixo;
    private final BigDecimal porKg;
    private final int prazoDias;

    FretePorPeso(String fixo, String porKg, int prazoDias) {
        this.fixo = new BigDecimal(fixo);
        this.porKg = new BigDecimal(porKg);
        this.prazoDias = prazoDias;
    }

    @Override
    public Entrega apurar(Pedido pedido) {
        BigDecimal frete = fixo.add(porKg.multiply(pedido.pesoKg()));
        return new Entrega(Dinheiro.centavos(frete), prazoDias);
    }
}
