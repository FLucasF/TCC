package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean parcelamentoValido(int numeroParcelas) {
        return numeroParcelas >= 1 && numeroParcelas <= 12;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int numeroParcelas) {
        if (numeroParcelas <= 3) {
            return new BigDecimal("0.00");
        }

        BigDecimal taxa = new BigDecimal("0.0199");

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal fatorPotencia = BigDecimal.ONE.divide(
            umMaisTaxa.pow(numeroParcelas),
            10,
            RoundingMode.HALF_EVEN
        );

        BigDecimal divisor = BigDecimal.ONE.subtract(fatorPotencia);

        BigDecimal parcela = totalPedido
            .multiply(taxa)
            .divide(divisor, 2, RoundingMode.HALF_EVEN);

        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(numeroParcelas));

        return totalComJuros.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int numeroParcelas) {
        return totalFinal
            .divide(new BigDecimal(numeroParcelas), 2, RoundingMode.HALF_EVEN);
    }
}
