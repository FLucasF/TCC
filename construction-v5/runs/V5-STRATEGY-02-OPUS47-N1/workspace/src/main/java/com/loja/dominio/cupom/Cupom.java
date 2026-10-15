package com.loja.dominio.cupom;

import com.loja.dominio.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotalProdutos);
    BigDecimal desconto(List<ItemCarrinho> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
