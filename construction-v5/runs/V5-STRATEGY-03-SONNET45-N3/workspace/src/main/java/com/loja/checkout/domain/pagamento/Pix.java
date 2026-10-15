package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Pix implements FormaPagamento {
    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(calcularAjuste(totalPedido, parcelas));
        return totalFinal.setScale(2, RoundingMode.HALF_EVEN);
    }
}
