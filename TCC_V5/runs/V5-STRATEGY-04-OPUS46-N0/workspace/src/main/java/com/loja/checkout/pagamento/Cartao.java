package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAX = 12;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAX;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela, BigDecimal.ZERO.setScale(2));
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(potencia, 34, RoundingMode.HALF_EVEN)
        );
        BigDecimal valorParcela = totalPedido.multiply(TAXA_MENSAL)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(totalFinal, valorParcela, ajuste);
    }
}
