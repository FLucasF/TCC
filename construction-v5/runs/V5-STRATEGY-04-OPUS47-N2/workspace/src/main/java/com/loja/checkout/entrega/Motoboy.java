package com.loja.checkout.entrega;

import com.loja.checkout.calculo.Dinheiro;
import com.loja.checkout.calculo.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_KG = new BigDecimal("5");

    @Override
    public boolean atende(List<ItemCarrinho> itens) {
        BigDecimal peso = itens.stream()
                .map(ItemCarrinho::pesoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return peso.compareTo(LIMITE_KG) <= 0;
    }

    @Override
    public BigDecimal frete(List<ItemCarrinho> itens) {
        return Dinheiro.cents(CUSTO);
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
