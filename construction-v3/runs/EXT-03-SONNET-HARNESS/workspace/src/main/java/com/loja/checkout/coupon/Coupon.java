package com.loja.checkout.coupon;

import com.loja.checkout.model.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

/** Um cupom: cada um sabe se se aplica ao pedido e quanto de desconto dá. */
public interface Coupon {

    String getCodigo();

    boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete);
}
