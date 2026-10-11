package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Pix implements FormaPagamento {

    @Override
    public String codigo() { return "PIX"; }

    @Override
    public boolean aceitaParcelas(int parcelas) { return parcelas == 1; }

    @Override
    public boolean disponivel(BigDecimal total) { return true; }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = total.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
    }
}
