package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAX = 12;

    @Override
    public String codigo() { return "CARTAO"; }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAX;
    }

    @Override
    public boolean disponivel(BigDecimal total) { return true; }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(new BigDecimal("0.00"), total, valorParcela);
        }

        // Tabela Price: parcela = total × taxa / (1 − (1+taxa)^−n)
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal inverso = BigDecimal.ONE.divide(potencia, 15, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(inverso);
        BigDecimal valorParcela = total.multiply(TAXA_JUROS)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);

        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ajuste = totalFinal.subtract(total).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
