package com.loja.checkout.domain.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public String codigo() {
        return "CARTAO";
    }

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
                    BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128));
        BigDecimal valorParcela = totalPedido
                .multiply(TAXA_MENSAL)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
