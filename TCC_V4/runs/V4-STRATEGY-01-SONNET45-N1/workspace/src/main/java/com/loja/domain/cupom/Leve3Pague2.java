package com.loja.domain.cupom;

import com.loja.model.ItemCarrinho;
import com.loja.util.Dinheiro;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal desconto = itens.stream()
            .map(item -> {
                int unidadesGratis = item.quantidade() / 3;
                return item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Dinheiro.arredondar(desconto);
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }
}
