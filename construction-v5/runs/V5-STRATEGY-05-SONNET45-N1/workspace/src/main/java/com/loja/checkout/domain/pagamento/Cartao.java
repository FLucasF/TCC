package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return new BigDecimal("0.00");
        }

        BigDecimal valorParcela = calcularValorParcelaComJuros(totalPedido, parcelas);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));

        return Dinheiro.arredondar(totalFinal.subtract(totalPedido));
    }

    @Override
    public int numeroParcelas(int parcelasDesejadas) {
        return parcelasDesejadas;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return Dinheiro.arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
    }

    private BigDecimal calcularValorParcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal fatorParcela = BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), 10, RoundingMode.HALF_EVEN);
        BigDecimal divisor = BigDecimal.ONE.subtract(fatorParcela);
        BigDecimal valorParcela = total.multiply(TAXA_JUROS).divide(divisor, 10, RoundingMode.HALF_EVEN);

        return Dinheiro.arredondar(valorParcela);
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }
}
