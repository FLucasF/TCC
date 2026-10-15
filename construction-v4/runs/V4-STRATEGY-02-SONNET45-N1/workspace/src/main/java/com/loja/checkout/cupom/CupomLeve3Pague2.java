package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal frete) {
        return itens.stream()
            .map(this::calcularDescontoItem)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean ehAplicavel(List<ItemCarrinho> itens) {
        return true;
    }

    private BigDecimal calcularDescontoItem(ItemCarrinho item) {
        int unidadesGratis = item.quantidade() / 3;
        return item.precoUnitario().multiply(new BigDecimal(unidadesGratis));
    }
}
