package com.loja.checkout.strategy;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    String getCodigo();
    boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
}
