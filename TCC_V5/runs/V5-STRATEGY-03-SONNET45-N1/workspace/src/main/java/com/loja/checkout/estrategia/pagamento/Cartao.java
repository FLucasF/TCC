package com.loja.checkout.estrategia.pagamento;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = calcularTotalComJuros(totalPedido, parcelas);
        return Moeda.arredondar(totalFinal.subtract(totalPedido));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return Moeda.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
        }

        BigDecimal fatorMultiplicador = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal fatorPotencia = fatorMultiplicador.pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA_JUROS_MENSAL).multiply(fatorPotencia);
        BigDecimal denominador = fatorPotencia.subtract(BigDecimal.ONE);
        BigDecimal valorParcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);

        return Moeda.arredondar(valorParcela);
    }

    private BigDecimal calcularTotalComJuros(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido;
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        return valorParcela.multiply(BigDecimal.valueOf(parcelas));
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }
}
