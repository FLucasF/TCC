package com.loja.domain.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
    boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens);
}
