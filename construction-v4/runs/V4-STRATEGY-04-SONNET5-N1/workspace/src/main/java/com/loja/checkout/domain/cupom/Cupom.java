package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Codificavel;
import com.loja.checkout.domain.Item;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom extends Codificavel {

    boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
