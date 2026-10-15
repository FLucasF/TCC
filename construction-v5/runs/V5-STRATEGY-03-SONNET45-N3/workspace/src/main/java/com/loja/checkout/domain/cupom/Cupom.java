package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
}
