package com.loja.service.coupon;

import com.loja.dto.ItemCarrinho;
import com.loja.util.MoneyRounder;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2CouponCalculator implements CouponCalculator {
    private final List<ItemCarrinho> itens;

    public Leve3Pague2CouponCalculator(List<ItemCarrinho> itens) {
        this.itens = itens;
    }

    @Override
    public void validate(BigDecimal subtotalProdutos, BigDecimal pesoTotal) {
    }

    @Override
    public BigDecimal calculate(BigDecimal subtotalProdutos, BigDecimal pesoTotal, BigDecimal frete) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int quantidade = item.quantidade();
            int gratuitos = quantidade / 3;
            if (gratuitos > 0) {
                BigDecimal discountForItem = item.precoUnitario().multiply(new BigDecimal(gratuitos));
                totalDiscount = totalDiscount.add(discountForItem);
            }
        }
        return MoneyRounder.round(totalDiscount);
    }
}
