package com.loja.strategy.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoPix implements FormaPagamento {
    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(PERCENTUAL_DESCONTO)
                .setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(calcularAjuste(totalPedido, parcelas));
    }
}
