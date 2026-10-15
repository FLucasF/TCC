package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int qtdGratis = item.quantidade() / 3;
            if (qtdGratis > 0) {
                desconto = desconto.add(
                        item.precoUnitario().multiply(BigDecimal.valueOf(qtdGratis))
                );
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {
    }
}
