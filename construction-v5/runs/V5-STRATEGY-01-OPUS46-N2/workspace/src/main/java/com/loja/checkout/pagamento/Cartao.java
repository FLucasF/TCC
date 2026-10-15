package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela, parcelas);
        }
        BigDecimal base = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal baseToN = base.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(baseToN);
        BigDecimal denominador = baseToN.subtract(BigDecimal.ONE);
        BigDecimal valorParcela = numerador.divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
    }
}
