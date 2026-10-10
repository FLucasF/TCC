package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CupomLeve3Pague2 implements Cupom {
    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, CheckoutRequest request) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, CheckoutRequest request) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemCarrinho item : request.itens()) {
            int unidadesGratis = item.quantidade() / 3;
            if (unidadesGratis > 0) {
                desconto = desconto.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis))
                );
            }
        }

        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
