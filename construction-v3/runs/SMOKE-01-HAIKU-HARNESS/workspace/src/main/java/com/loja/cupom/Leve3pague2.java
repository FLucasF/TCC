package com.loja.cupom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.loja.model.Item;
import java.util.List;

public class Leve3pague2 implements Cupom {
    @Override
    public boolean isApplicable(List<Item> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal discount = BigDecimal.ZERO;

        for (Item item : itens) {
            if (item.getQuantidade() != null && item.getQuantidade() > 0 &&
                item.getPrecoUnitario() != null && item.getPrecoUnitario().compareTo(BigDecimal.ZERO) > 0) {

                int quantidade = item.getQuantidade();
                int quantidadePaga = (quantidade / 3) * 2 + (quantidade % 3);
                int quantidadeGratis = quantidade - quantidadePaga;

                BigDecimal itemDiscount = new BigDecimal(quantidadeGratis)
                        .multiply(item.getPrecoUnitario());

                discount = discount.add(itemDiscount);
            }
        }

        return discount.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String getCode() {
        return "LEVE3PAGUE2";
    }
}
