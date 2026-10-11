package com.loja.strategy.pagamento;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal valorParcela = calcularValorParcela(total, parcelas);
        BigDecimal totalComJuros = valorParcela.multiply(BigDecimal.valueOf(parcelas));

        return totalComJuros.subtract(total);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return Arredondamento.arredondar(
                total.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN)
            );
        }

        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal divisor = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), 10, RoundingMode.HALF_EVEN)
        );

        return Arredondamento.arredondar(
            total.multiply(taxa).divide(divisor, 10, RoundingMode.HALF_EVEN)
        );
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean aceita(BigDecimal total) {
        return true;
    }
}
