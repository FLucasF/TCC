package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Base para as opcoes que cobram uma taxa fixa mais um valor por kg do pedido. */
public abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal taxaFixa;
    private final BigDecimal valorPorKg;
    private final int prazoDias;

    protected FretePorPeso(String taxaFixa, String valorPorKg, int prazoDias) {
        this.taxaFixa = new BigDecimal(taxaFixa);
        this.valorPorKg = new BigDecimal(valorPorKg);
        this.prazoDias = prazoDias;
    }

    @Override
    public int prazoDias(Pedido pedido) {
        return prazoDias;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.valor(taxaFixa.add(valorPorKg.multiply(pedido.pesoKg())));
    }
}
