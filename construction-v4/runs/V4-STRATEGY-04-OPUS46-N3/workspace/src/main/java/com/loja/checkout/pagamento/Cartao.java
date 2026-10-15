package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela, BigDecimal.ZERO.setScale(2));
        }

        double taxa = TAXA_MENSAL.doubleValue();
        double fator = taxa / (1 - Math.pow(1 + taxa, -parcelas));
        BigDecimal valorParcela = totalPedido.multiply(BigDecimal.valueOf(fator)).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(totalFinal, valorParcela, ajuste);
    }
}
