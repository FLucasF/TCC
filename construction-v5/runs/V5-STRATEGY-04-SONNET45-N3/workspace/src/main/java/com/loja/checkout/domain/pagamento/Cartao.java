package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Arredondamento;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public boolean aceitaParcelas(int numeroParcelas) {
        return numeroParcelas >= 1 && numeroParcelas <= 12;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int numeroParcelas) {
        BigDecimal valorFinal = calcularValorFinal(totalPedido, numeroParcelas);
        return valorFinal.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int numeroParcelas) {
        if (numeroParcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido;
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, numeroParcelas);
        BigDecimal valorFinal = valorParcela.multiply(BigDecimal.valueOf(numeroParcelas));
        return Arredondamento.arredondar(valorFinal);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int numeroParcelas) {
        if (numeroParcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido.divide(
                BigDecimal.valueOf(numeroParcelas),
                2,
                RoundingMode.HALF_EVEN
            );
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal denominador = umMaisTaxa.pow(numeroParcelas);
        BigDecimal fator = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(denominador, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal parcela = totalPedido.multiply(TAXA_JUROS_MENSAL)
            .divide(fator, 10, RoundingMode.HALF_EVEN);

        return Arredondamento.arredondar(parcela);
    }
}
