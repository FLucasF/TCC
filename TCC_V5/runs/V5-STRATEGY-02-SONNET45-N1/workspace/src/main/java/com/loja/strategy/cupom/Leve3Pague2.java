package com.loja.strategy.cupom;

import com.loja.domain.Item;
import com.loja.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    private final List<Item> itens;

    public Leve3Pague2(List<Item> itens) {
        this.itens = itens;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            desconto = desconto.add(
                item.getPrecoUnitario().multiply(BigDecimal.valueOf(unidadesGratis))
            );
        }
        return Arredondamento.arredondar(desconto);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
