package com.loja.pagamento;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class FormaPagamentoCartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = BigDecimal.valueOf(0.0199);

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        if (parcelas == null || parcelas <= 0) {
            parcelas = 1;
        }

        if (parcelas <= 3) {
            return new BigDecimal("0.00");
        }

        BigDecimal parcela = calcularParcelaComJuros(total, parcelas);
        BigDecimal totalComJuros = parcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalComJuros.subtract(total);
        return Arredondamento.arredondarParaPar(ajuste);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, Integer parcelas) {
        double taxa = TAXA_JUROS.doubleValue();
        int n = parcelas;

        double denominador = 1.0 - Math.pow(1.0 + taxa, -n);
        double parcela = total.doubleValue() * taxa / denominador;

        return Arredondamento.arredondarParaPar(parcela);
    }

    @Override
    public boolean podeParcelar() {
        return true;
    }

    @Override
    public Integer getMaximoParcelas() {
        return 12;
    }

    @Override
    public boolean estaDisponivel(BigDecimal total) {
        return true;
    }
}
