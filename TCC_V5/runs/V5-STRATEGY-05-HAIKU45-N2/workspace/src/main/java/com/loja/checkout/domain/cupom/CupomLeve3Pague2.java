package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Arredondamento;

public class CupomLeve3Pague2 implements EstrategiaCupom {
    @Override
    public boolean aplicavel(BigDecimal subtotal, List<Item> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int quantidadeGratisDesteItem = item.getQuantidade() / 3;
            if (quantidadeGratisDesteItem > 0) {
                desconto = desconto.add(
                    item.getPrecoUnitario().multiply(new BigDecimal(quantidadeGratisDesteItem))
                );
            }
        }
        return Arredondamento.arredondar(desconto);
    }

    @Override
    public int descricao() {
        return 0;
    }
}
