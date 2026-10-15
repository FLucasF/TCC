package com.loja.checkout.pagamento;

import com.loja.checkout.calculo.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final BigDecimal UM_MAIS_TAXA = BigDecimal.ONE.add(TAXA);
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final int SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= SEM_JUROS) {
            BigDecimal parcela = Dinheiro.cents(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN));
            BigDecimal totalFinal = Dinheiro.cents(parcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = Dinheiro.cents(totalFinal.subtract(totalPedido));
            return new ResultadoPagamento(totalFinal, parcelas, parcela, ajuste);
        }
        BigDecimal fator = UM_MAIS_TAXA.pow(parcelas, MC);
        BigDecimal denom = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MC), MC);
        BigDecimal parcelaBruta = totalPedido.multiply(TAXA, MC).divide(denom, MC);
        BigDecimal parcela = Dinheiro.cents(parcelaBruta);
        BigDecimal totalFinal = Dinheiro.cents(parcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Dinheiro.cents(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(totalFinal, parcelas, parcela, ajuste);
    }
}
