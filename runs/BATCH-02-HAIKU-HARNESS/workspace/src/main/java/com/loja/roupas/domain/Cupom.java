package com.loja.roupas.domain;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean podeAplicar(BigDecimal subtotal, Double pesoTotal, List<ItemPedido> itens);
    BigDecimal calcularDesconto(BigDecimal subtotal, Double pesoTotal, BigDecimal frete, List<ItemPedido> itens);
}
