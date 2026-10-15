package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomBemvindo10 implements Cupom {
    private static final BigDecimal DESCONTO_PERCENTUAL = new BigDecimal("0.10");

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal frete) {
        BigDecimal subtotal = calcularSubtotalProdutos(itens);
        return subtotal.multiply(DESCONTO_PERCENTUAL)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean ehAplicavel(List<ItemCarrinho> itens) {
        return true;
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
