package com.loja.checkout.modelo.cupom;

import com.loja.checkout.dto.ItemPedidoDTO;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos, BigDecimal frete);
    boolean ehAplicavel(List<ItemPedidoDTO> itens, BigDecimal subtotalProdutos);
}
