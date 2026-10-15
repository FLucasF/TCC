package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = calcularTotalFinal(totalPedido, parcelas);
        return totalFinal.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        // Tabela Price: parcela = total × taxa ÷ (1 − (1 + taxa)^−parcelas)
        BigDecimal taxa = TAXA_JUROS_MENSAL;
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal valorParcela = totalPedido.multiply(taxa)
            .divide(denominador, 2, RoundingMode.HALF_EVEN);

        return valorParcela;
    }

    private BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido;
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        return valorParcela.multiply(BigDecimal.valueOf(parcelas));
    }
}
