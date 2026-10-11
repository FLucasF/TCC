package com.loja.checkout.domain.cupom;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean ehAplicavel(List<ItemCarrinho> itens, BigDecimal subtotalProdutos);
    BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotalProdutos, BigDecimal valorFrete);
}
