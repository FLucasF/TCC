package com.loja.checkout.clube;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BeneficiosPrata implements BeneficioClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(List<ItemCarrinho> itens) {
        BigDecimal subtotal = calcularSubtotalProdutos(itens);
        return subtotal.multiply(PERCENTUAL_CREDITO)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean ganharBrinde(BigDecimal subtotalProdutos) {
        return false;
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
