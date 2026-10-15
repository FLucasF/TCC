package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela, BigDecimal.ZERO.setScale(2));
        }

        double total = totalPedido.doubleValue();
        double taxa = TAXA_MENSAL.doubleValue();
        double valorParcela = total * taxa / (1 - Math.pow(1 + taxa, -parcelas));

        BigDecimal parcelaArredondada = BigDecimal.valueOf(valorParcela).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = parcelaArredondada.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);

        return new ResultadoPagamento(totalFinal, parcelaArredondada, ajuste);
    }
}
