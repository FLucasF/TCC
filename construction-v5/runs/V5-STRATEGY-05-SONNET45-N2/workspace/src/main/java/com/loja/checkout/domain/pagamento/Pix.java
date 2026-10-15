package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean aceitaParcelas(int numeroParcelas) {
        return numeroParcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = calcularAjuste(totalPedido, parcelas);
        return totalPedido.add(ajuste);
    }
}
