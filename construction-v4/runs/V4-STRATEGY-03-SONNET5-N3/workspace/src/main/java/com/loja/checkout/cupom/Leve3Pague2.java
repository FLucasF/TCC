package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_LEVA = 3;

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal desconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = item.quantidade() / TAMANHO_LEVA;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return desconto;
    }
}
