package com.loja.pagamento;

import com.loja.util.Arredondador;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean aceita(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal valorParcela = calcularParcelaComJuros(totalPedido, parcelas);
        BigDecimal totalComJuros = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return totalComJuros.subtract(totalPedido);
    }

    @Override
    public int numeroParcelas(int parcelasInformadas) {
        return parcelasInformadas;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return Arredondador.arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
        }

        BigDecimal totalSemJuros = totalFinal.divide(
            BigDecimal.ONE.add(calcularFatorJuros(parcelas)),
            10,
            RoundingMode.HALF_EVEN
        );
        return calcularParcelaComJuros(totalSemJuros, parcelas);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal valor, int parcelas) {
        BigDecimal fator = calcularFatorJuros(parcelas);
        BigDecimal numerador = valor.multiply(TAXA_JUROS);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(
                BigDecimal.ONE.add(TAXA_JUROS).pow(parcelas),
                10,
                RoundingMode.HALF_EVEN
            )
        );
        BigDecimal parcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);
        return Arredondador.arredondar(parcela);
    }

    private BigDecimal calcularFatorJuros(int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal parcelaUnitaria = TAXA_JUROS.multiply(potencia)
            .divide(potencia.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_EVEN);
        return parcelaUnitaria.multiply(BigDecimal.valueOf(parcelas)).subtract(BigDecimal.ONE);
    }
}
