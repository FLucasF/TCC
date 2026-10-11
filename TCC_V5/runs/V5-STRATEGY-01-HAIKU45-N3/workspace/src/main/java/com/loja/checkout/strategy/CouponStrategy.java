package com.loja.checkout.strategy;

import com.loja.checkout.dto.Item;
import java.math.BigDecimal;
import java.util.List;

public interface CouponStrategy {
    boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteCalculado);
}
