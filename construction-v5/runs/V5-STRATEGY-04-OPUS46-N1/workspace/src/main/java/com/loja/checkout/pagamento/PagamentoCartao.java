package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class PagamentoCartao implements CalculoPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final MathContext MC = MathContext.DECIMAL128;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela, parcelas);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, MC);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, MC));
        BigDecimal valorParcela = totalPedido.multiply(TAXA_MENSAL, MC)
                .divide(divisor, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }
}
