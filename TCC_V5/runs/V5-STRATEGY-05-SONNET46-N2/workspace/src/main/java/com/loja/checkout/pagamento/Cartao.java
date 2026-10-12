package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean aceita(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, parcelas, valorParcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal potenciaNegativa = BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(potenciaNegativa);
        BigDecimal valorParcela = totalPedido.multiply(TAXA_JUROS)
                .divide(denominador, MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, parcelas, valorParcela);
    }
}
