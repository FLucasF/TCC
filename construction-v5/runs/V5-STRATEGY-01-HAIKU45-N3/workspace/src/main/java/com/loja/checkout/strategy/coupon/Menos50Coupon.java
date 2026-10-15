package com.loja.checkout.strategy.coupon;

import com.loja.checkout.dto.Item;
import com.loja.checkout.strategy.CouponStrategy;
import java.math.BigDecimal;
import java.util.List;

public class Menos50Coupon implements CouponStrategy {
    @Override
    public boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteCalculado) {
        return new BigDecimal("50.00");
    }
}
