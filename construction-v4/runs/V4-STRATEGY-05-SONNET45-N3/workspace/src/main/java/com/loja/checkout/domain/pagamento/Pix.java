package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Pix implements FormaPagamento {

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean parcelamentoValido(int numeroParcelas) {
        return numeroParcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int numeroParcelas) {
        BigDecimal desconto = totalPedido
            .multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int numeroParcelas) {
        return totalFinal;
    }
}
