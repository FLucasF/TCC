package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCupom> itens);

    boolean aplicavel(BigDecimal subtotalProdutos);

    record ItemCupom(String nome, BigDecimal precoUnitario, int quantidade) {}
}
