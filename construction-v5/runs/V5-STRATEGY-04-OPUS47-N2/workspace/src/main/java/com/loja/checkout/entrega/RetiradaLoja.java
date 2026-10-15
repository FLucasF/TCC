package com.loja.checkout.entrega;

import com.loja.checkout.calculo.Dinheiro;
import com.loja.checkout.calculo.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public boolean atende(List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal frete(List<ItemCarrinho> itens) {
        return Dinheiro.zero();
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
