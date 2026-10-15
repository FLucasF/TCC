package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela);
        }
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal potenciaNeg = BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(potenciaNeg);
        BigDecimal valorParcela = totalPedido.multiply(TAXA_MENSAL)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
