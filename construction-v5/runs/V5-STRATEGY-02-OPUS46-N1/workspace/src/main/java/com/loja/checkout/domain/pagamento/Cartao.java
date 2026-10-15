package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final double TAXA_MENSAL = 0.0199;

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido
                    .divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
        }

        double total = totalPedido.doubleValue();
        double parcela = total * TAXA_MENSAL / (1 - Math.pow(1 + TAXA_MENSAL, -parcelas));
        BigDecimal valorParcela = BigDecimal.valueOf(parcela).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));

        return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
    }
}
