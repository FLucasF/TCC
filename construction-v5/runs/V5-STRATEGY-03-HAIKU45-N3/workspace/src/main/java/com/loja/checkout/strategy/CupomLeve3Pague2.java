package com.loja.checkout.strategy;

import com.loja.checkout.model.Item;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements CupomStrategy {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
        BigDecimal desconto = new BigDecimal("0.00");

        for (Item item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;

            BigDecimal preco = new BigDecimal(item.getPrecoUnitario());
            desconto = desconto.add(preco.multiply(new BigDecimal(unidadesGratis)));
        }

        return desconto;
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal) {
        return true;
    }
}
