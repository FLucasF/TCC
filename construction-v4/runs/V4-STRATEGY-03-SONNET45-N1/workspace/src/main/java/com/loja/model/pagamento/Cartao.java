package com.loja.model.pagamento;

import com.loja.util.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido;
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal fator = umMaisTaxa.pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, 10, RoundingMode.HALF_EVEN));

        BigDecimal valorParcela = totalPedido.multiply(TAXA_JUROS).divide(divisor, 10, RoundingMode.HALF_EVEN);
        valorParcela = Dinheiro.arredondar(valorParcela);

        return valorParcela.multiply(BigDecimal.valueOf(parcelas));
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return true;
    }
}
