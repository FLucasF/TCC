package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, BigDecimal.ZERO.setScale(2), parcelas, valorParcela);
        }
        BigDecimal onePlusRate = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal fatorDesconto = BigDecimal.ONE.divide(
            onePlusRate.pow(parcelas), 20, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
        BigDecimal valorParcela = totalPedido.multiply(TAXA_JUROS)
            .divide(denominador, 20, RoundingMode.HALF_EVEN)
            .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(totalFinal, ajuste, parcelas, valorParcela);
    }
}
