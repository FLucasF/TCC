package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
    boolean aplicavel(BigDecimal subtotalProdutos);
}
