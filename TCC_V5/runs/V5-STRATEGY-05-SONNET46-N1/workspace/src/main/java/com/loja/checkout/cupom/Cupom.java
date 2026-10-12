package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    String codigo();
    void validarAplicabilidade(BigDecimal subtotalProdutos);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete);
}
