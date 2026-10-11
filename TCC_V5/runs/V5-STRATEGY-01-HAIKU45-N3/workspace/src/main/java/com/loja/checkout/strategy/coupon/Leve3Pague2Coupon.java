package com.loja.checkout.strategy.coupon;

import com.loja.checkout.dto.Item;
import com.loja.checkout.strategy.CouponStrategy;
import com.loja.checkout.util.MoneyRounder;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2Coupon implements CouponStrategy {
    @Override
    public boolean estaAplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteCalculado) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (Item item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return MoneyRounder.round(desconto);
    }
}
