package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Cartao implements Pagamento {
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final int SEM_JUROS_ATE = 3;
    private static final int MAX_PARCELAS = 12;
    private static final MathContext MC = MathContext.DECIMAL128;

    @Override public String codigo() { return "CARTAO"; }
    @Override public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }
    @Override public boolean disponivelPara(BigDecimal totalPedido) { return true; }

    @Override public Ajuste calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= SEM_JUROS_ATE) {
            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), MC));
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido);
            return new Ajuste(Dinheiro.ZERO, totalFinal, parcela);
        }
        BigDecimal um = BigDecimal.ONE;
        BigDecimal base = um.add(TAXA);
        BigDecimal baseElevado = base.pow(parcelas, MC);
        BigDecimal denominador = um.subtract(um.divide(baseElevado, MC));
        BigDecimal parcela = totalPedido.multiply(TAXA, MC).divide(denominador, MC);
        BigDecimal parcelaArred = parcela.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = Dinheiro.arredondar(
                parcelaArred.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
        return new Ajuste(ajuste, totalFinal, parcelaArred);
    }
}
