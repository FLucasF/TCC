package com.loja.dominio.pagamento;

import com.loja.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final double TAXA_D = 0.0199;
    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override public boolean atende(BigDecimal totalPedido) { return true; }

    @Override public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido);
            BigDecimal parcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN)
            );
            return new ResultadoPagamento(Dinheiro.ZERO, totalFinal, parcela);
        }
        double fator = Math.pow(1.0 + TAXA_D, -parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.valueOf(fator));
        BigDecimal parcela = Dinheiro.centavos(
            totalPedido.multiply(TAXA).divide(divisor, 10, RoundingMode.HALF_EVEN)
        );
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Dinheiro.centavos(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, parcela);
    }
}
