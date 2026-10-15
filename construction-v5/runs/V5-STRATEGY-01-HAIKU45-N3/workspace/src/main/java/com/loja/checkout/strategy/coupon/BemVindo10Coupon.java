package com.loja.checkout.strategy.coupon;

import com.loja.checkout.dto.Item;
import com.loja.checkout.strategy.CouponStrategy;
import com.loja.checkout.util.MoneyRounder;
import java.math.BigDecimal;
import java.util.List;

public class BemVindo10Coupon implements CouponStrategy {
    @Override
    public boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteCalculado) {
        BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
        return MoneyRounder.round(desconto);
    }
}
