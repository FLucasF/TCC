package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(
                    new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal potenciaInversa = BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(potenciaInversa);
        BigDecimal valorParcela = totalPedido
                .multiply(TAXA)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
