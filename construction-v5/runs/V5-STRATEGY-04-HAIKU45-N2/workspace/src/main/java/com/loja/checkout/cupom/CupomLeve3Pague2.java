package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;
import com.loja.checkout.util.Arredonda;

public class CupomLeve3Pague2 implements CalculadoraCupom {
    @Override
    public BigDecimal calcular(List<Item> itens, BigDecimal subtotal) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (Item item : itens) {
            int quantidadeGratis = item.quantidade / 3;
            desconto = desconto.add(new BigDecimal(quantidadeGratis).multiply(new BigDecimal(String.valueOf(item.precoUnitario))));
        }

        return Arredonda.arredonda(desconto);
    }

    @Override
    public int getPrazoEntrega() {
        return 0;
    }
}
