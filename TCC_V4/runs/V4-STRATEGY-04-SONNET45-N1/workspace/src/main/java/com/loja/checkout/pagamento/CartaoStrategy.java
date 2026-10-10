package com.loja.checkout.pagamento;

import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CartaoStrategy implements PagamentoStrategy {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = calcularTotalFinal(totalPedido, parcelas);
        return Arredondamento.arredondar(totalFinal.subtract(totalPedido));
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido;
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        return Arredondamento.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return Arredondamento.arredondar(
                totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN)
            );
        }

        BigDecimal n = BigDecimal.valueOf(parcelas);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal umMaisTaxaElevadoN = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxaElevadoN, 10, RoundingMode.HALF_EVEN)
        );
        BigDecimal valorParcela = totalPedido.multiply(TAXA_JUROS).divide(denominador, 10, RoundingMode.HALF_EVEN);

        return Arredondamento.arredondar(valorParcela);
    }
}
