package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Base das modalidades que cobram um valor fixo mais um valor por kg do pedido. */
abstract class ModalidadePorPeso implements ModalidadeEntrega {

    private final String codigo;
    private final BigDecimal fixo;
    private final BigDecimal porKg;
    private final int prazoDias;

    protected ModalidadePorPeso(String codigo, String fixo, String porKg, int prazoDias) {
        this.codigo = codigo;
        this.fixo = new BigDecimal(fixo);
        this.porKg = new BigDecimal(porKg);
        this.prazoDias = prazoDias;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return true;
    }

    @Override
    public Entrega calcular(Carrinho carrinho) {
        BigDecimal frete = fixo.add(porKg.multiply(carrinho.pesoKg()));
        return new Entrega(Dinheiro.centavos(frete), prazoDias);
    }
}
