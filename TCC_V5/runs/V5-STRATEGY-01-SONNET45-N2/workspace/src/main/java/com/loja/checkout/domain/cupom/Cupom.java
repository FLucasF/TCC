package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public interface Cupom {
    boolean aplicavel(double subtotalProdutos, List<ItemPedido> itens);
    ResultadoCupom calcular(double subtotalProdutos, double frete, List<ItemPedido> itens);
}
