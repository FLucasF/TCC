package com.loja.checkout.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FormaPagamentoCartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean aceitaTotal(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal valorParcela = calcularParcelaComJuros(totalPedido, parcelas);
        BigDecimal totalComJuros = valorParcela.multiply(new BigDecimal(parcelas));
        return totalComJuros.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal ajuste, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        return calcularParcelaComJuros(totalPedido, parcelas);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxaElevado, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal valorParcela = totalPedido.multiply(TAXA_JUROS).divide(denominador, 2, RoundingMode.HALF_EVEN);
        return valorParcela;
    }
}
