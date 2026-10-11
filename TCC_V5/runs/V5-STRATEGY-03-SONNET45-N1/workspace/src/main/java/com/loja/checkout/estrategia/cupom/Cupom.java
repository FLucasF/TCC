package com.loja.checkout.estrategia.cupom;

import com.loja.checkout.dto.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
    boolean eAplicavel(BigDecimal subtotalProdutos);
}
