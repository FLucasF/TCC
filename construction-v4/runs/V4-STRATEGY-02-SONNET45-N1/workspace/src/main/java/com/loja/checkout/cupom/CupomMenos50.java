package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomMenos50 implements Cupom {
    private static final BigDecimal DESCONTO_VALOR = new BigDecimal("50.00");
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal frete) {
        return DESCONTO_VALOR;
    }

    @Override
    public boolean ehAplicavel(List<ItemCarrinho> itens) {
        BigDecimal subtotal = calcularSubtotalProdutos(itens);
        return subtotal.compareTo(VALOR_MINIMO) >= 0;
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
