package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public boolean aceitaParcelas(int numParcelas) {
        return numParcelas >= PARCELAS_MINIMAS && numParcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return calcularSemJuros(totalPedido, parcelas);
        } else {
            return calcularComJuros(totalPedido, parcelas);
        }
    }

    private ResultadoPagamento calcularSemJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Arredondamento.arredondar(
            totalPedido.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN)
        );

        return new ResultadoPagamento(new BigDecimal("0.00"), totalPedido, valorParcela);
    }

    private ResultadoPagamento calcularComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxaElevado, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal valorParcela = Arredondamento.arredondar(
            totalPedido.multiply(TAXA_JUROS_MENSAL).divide(denominador, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);

        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
