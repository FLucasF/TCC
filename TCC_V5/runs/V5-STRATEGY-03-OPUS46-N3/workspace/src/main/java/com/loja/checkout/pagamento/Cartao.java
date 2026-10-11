package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela, BigDecimal.ZERO.setScale(2), parcelas);
        }

        BigDecimal um = BigDecimal.ONE;
        BigDecimal basePotencia = um.add(TAXA_MENSAL).pow(parcelas, MathContext.DECIMAL128);
        BigDecimal denominador = um.subtract(um.divide(basePotencia, 34, RoundingMode.HALF_EVEN));
        BigDecimal valorParcela = totalPedido.multiply(TAXA_MENSAL)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(totalFinal, valorParcela, ajuste, parcelas);
    }
}
