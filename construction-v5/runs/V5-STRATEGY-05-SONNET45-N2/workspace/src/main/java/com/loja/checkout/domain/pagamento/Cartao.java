package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean aceitaParcelas(int numeroParcelas) {
        return numeroParcelas >= 1 && numeroParcelas <= 12;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO;
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));

        return totalFinal.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
        }

        BigDecimal fatorParcela = calcularFatorParcela(parcelas);
        return Dinheiro.arredondar(totalPedido.multiply(fatorParcela));
    }

    private BigDecimal calcularFatorParcela(int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN));

        return TAXA_JUROS.divide(denominador, 10, RoundingMode.HALF_EVEN);
    }
}
