package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.AjustePagamento;
import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final int SEM_JUROS_MAX = 3;
    private static final int MAX_PARCELAS = 12;
    private static final int ESCALA_CALCULO = 12;

    @Override public boolean parcelasValidas(int parcelas) { return parcelas >= 1 && parcelas <= MAX_PARCELAS; }
    @Override public boolean aceita(BigDecimal totalPedido) { return true; }

    @Override public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= SEM_JUROS_MAX) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new AjustePagamento(Dinheiro.ZERO, parcela);
        }
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal numerador = totalPedido.multiply(TAXA).multiply(potencia);
        BigDecimal denominador = potencia.subtract(BigDecimal.ONE);
        BigDecimal parcela = Dinheiro.arredondar(numerador.divide(denominador, ESCALA_CALCULO, RoundingMode.HALF_EVEN));
        BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new AjustePagamento(ajuste, parcela);
    }
}
