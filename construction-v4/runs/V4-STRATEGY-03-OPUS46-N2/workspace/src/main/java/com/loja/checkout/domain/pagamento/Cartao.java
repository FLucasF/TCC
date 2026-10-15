package com.loja.checkout.domain.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(
                    BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela, parcelas);
        }

        double taxa = TAXA_MENSAL.doubleValue();
        double fator = Math.pow(1 + taxa, -parcelas);
        double parcelaDouble = totalPedido.doubleValue() * taxa / (1 - fator);

        BigDecimal valorParcela = BigDecimal.valueOf(parcelaDouble).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));

        return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
    }
}
