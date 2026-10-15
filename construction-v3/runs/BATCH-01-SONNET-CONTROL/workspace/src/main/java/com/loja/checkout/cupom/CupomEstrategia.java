package com.loja.checkout.cupom;

import com.loja.checkout.service.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

public interface CupomEstrategia {

    String getCodigo();

    boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
