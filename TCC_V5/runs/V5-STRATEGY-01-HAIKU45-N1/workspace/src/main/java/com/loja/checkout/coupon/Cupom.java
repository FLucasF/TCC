package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public interface Cupom {
    double calcularDesconto(double subtotalProdutos, List<ItemPedido> itens);
    boolean estaAplicavel(double subtotalProdutos, List<ItemPedido> itens);
    String getCodigo();
}
