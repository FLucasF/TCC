package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            BigDecimal descontoItem = BigDecimal.valueOf(item.precoUnitario())
                .multiply(BigDecimal.valueOf(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
