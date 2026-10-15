package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinhoDto;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String getCodigo();

    boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinhoDto> itens);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinhoDto> itens);
}
