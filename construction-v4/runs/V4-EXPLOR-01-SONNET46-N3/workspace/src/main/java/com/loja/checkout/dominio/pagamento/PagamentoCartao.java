package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.infra.CheckoutException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class PagamentoCartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > 12) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal total) {
        // sem restrição
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), total, parcelas, valorParcela);
        }

        // Tabela Price: parcela = total × taxa / (1 − (1 + taxa)^−n)
        double r = TAXA_MENSAL.doubleValue();
        double fator = 1.0 - Math.pow(1.0 + r, -parcelas);
        BigDecimal valorParcela = total.multiply(TAXA_MENSAL)
                .divide(BigDecimal.valueOf(fator), 10, RoundingMode.HALF_EVEN)
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(total);
        return new ResultadoPagamento(ajuste, totalFinal, parcelas, valorParcela);
    }
}
