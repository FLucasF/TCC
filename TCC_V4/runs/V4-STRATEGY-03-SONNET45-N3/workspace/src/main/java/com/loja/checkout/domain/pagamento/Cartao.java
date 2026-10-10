package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido;
        }

        BigDecimal numerador = totalPedido.multiply(TAXA_JUROS);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal umMaisTaxaElevadoNegativo = BigDecimal.ONE.divide(
            umMaisTaxaElevado, 20, RoundingMode.HALF_EVEN
        );
        BigDecimal denominador = BigDecimal.ONE.subtract(umMaisTaxaElevadoNegativo);

        BigDecimal valorParcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN)
            .setScale(2, RoundingMode.HALF_EVEN);
        return valorParcela.multiply(new BigDecimal(parcelas));
    }

    @Override
    public boolean verificarDisponibilidade(BigDecimal totalPedido, int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }
}
