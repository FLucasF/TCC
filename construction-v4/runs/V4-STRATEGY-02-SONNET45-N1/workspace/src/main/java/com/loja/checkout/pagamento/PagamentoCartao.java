package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoCartao implements Pagamento {
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal valorFinal = calcularValorFinalComJuros(totalPedido, parcelas);
        return valorFinal.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            return totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        }
        return calcularParcelaComJuros(totalPedido, parcelas);
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal fatorPotencia = umMaisTaxa.pow(parcelas);
        BigDecimal fatorInverso = BigDecimal.ONE.divide(fatorPotencia, 10, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorInverso);
        return totalPedido.multiply(TAXA_JUROS_MENSAL)
            .divide(denominador, 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularValorFinalComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = calcularParcelaComJuros(totalPedido, parcelas);
        return parcela.multiply(new BigDecimal(parcelas));
    }
}
