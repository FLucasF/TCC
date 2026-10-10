package com.loja.model.cupom;

import com.loja.dto.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete);
    boolean aplicavel(BigDecimal subtotalProdutos);
}
