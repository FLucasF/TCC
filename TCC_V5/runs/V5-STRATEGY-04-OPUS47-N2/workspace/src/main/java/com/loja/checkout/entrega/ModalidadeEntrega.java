package com.loja.checkout.entrega;

import com.loja.checkout.calculo.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public interface ModalidadeEntrega {
    boolean atende(List<ItemCarrinho> itens);
    BigDecimal frete(List<ItemCarrinho> itens);
    int prazoDias();
}
