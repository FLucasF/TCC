package com.loja.domain.pagamento;

import com.loja.util.Moeda;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

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
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Moeda.arredondar(
                totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN)
            );
            return new ResultadoPagamento(new BigDecimal("0.00"), totalPedido, valorParcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal umMaisTaxaPow = umMaisTaxa;
        for (int i = 1; i < parcelas; i++) {
            umMaisTaxaPow = umMaisTaxaPow.multiply(umMaisTaxa);
        }
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxaPow, 10, RoundingMode.HALF_EVEN)
        );
        BigDecimal valorParcela = Moeda.arredondar(
            totalPedido.multiply(TAXA_JUROS).divide(denominador, 10, RoundingMode.HALF_EVEN)
        );
        BigDecimal totalFinal = Moeda.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);

        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
