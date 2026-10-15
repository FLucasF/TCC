package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemPedidoRequest;

import java.math.BigDecimal;
import java.util.List;

public interface CalculadoraCupom {

    String getCodigo();

    boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
