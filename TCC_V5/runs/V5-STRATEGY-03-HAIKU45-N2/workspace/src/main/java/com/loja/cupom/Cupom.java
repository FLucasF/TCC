package com.loja.cupom;

import com.loja.model.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete);
    boolean podeAplicar(List<ItemPedido> itens, BigDecimal subtotal);
    String obterCodigo();
}
