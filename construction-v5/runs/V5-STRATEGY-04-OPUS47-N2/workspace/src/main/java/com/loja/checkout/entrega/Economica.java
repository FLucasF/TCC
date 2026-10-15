package com.loja.checkout.entrega;

import com.loja.checkout.calculo.Dinheiro;
import com.loja.checkout.calculo.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Economica implements ModalidadeEntrega {
    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public boolean atende(List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal frete(List<ItemCarrinho> itens) {
        BigDecimal peso = itens.stream()
                .map(ItemCarrinho::pesoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.cents(BASE.add(POR_KG.multiply(peso)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
