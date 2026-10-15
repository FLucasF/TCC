package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.model.ItemRequest;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens);
    boolean ehAplicavel(BigDecimal subtotalProdutos);
    boolean ofereceFretGratis();
    String codigo();
}
