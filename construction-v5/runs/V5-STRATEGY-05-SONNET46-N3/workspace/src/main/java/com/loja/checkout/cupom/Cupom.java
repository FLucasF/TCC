package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
    void validarAplicabilidade(BigDecimal subtotalProdutos);
}
