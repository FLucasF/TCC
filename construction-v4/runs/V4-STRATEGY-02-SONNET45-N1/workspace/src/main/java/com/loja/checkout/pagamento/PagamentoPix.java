package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoPix implements Pagamento {
    private static final BigDecimal DESCONTO_PERCENTUAL = new BigDecimal("0.05");

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(DESCONTO_PERCENTUAL)
            .setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = calcularAjuste(totalPedido, parcelas);
        return totalPedido.add(ajuste);
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }
}
