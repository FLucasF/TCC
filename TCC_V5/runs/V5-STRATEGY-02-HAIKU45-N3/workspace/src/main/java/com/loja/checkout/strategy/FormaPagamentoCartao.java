package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.util.ArredondadorMeio;

public class FormaPagamentoCartao implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        BigDecimal valorParcela = calcularValorParcela(total, parcelas);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        return totalFinal.subtract(total);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return ArredondadorMeio.arredondar(total.divide(new BigDecimal(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
        }

        BigDecimal taxaMensal = new BigDecimal("0.0199");
        BigDecimal numerador = total.multiply(taxaMensal);

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
        BigDecimal potencia = umMaisTaxa.pow(-parcelas, new java.math.MathContext(20));
        BigDecimal denominador = BigDecimal.ONE.subtract(potencia);

        BigDecimal parcela = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
        return ArredondadorMeio.arredondar(parcela);
    }

    @Override
    public boolean parcelaPermitida(Integer parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean podeAtender(BigDecimal total) {
        return true;
    }

    @Override
    public String nome() {
        return "CARTAO";
    }

}
