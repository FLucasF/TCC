package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Pix implements FormaPagamento {
    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(TAXA_DESCONTO).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(totalFinal, desconto.negate(), 1, totalFinal);
    }
}
