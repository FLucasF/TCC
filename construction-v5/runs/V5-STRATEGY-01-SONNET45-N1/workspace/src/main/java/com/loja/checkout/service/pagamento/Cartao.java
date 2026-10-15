package com.loja.checkout.service.pagamento;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceita(BigDecimal totalPedido, int parcelas) {
        return true;
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return calcularValorFinal(totalPedido, parcelas).subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido;
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        return valorParcela.multiply(BigDecimal.valueOf(parcelas));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN)
        );

        return totalPedido.multiply(TAXA_JUROS)
            .divide(denominador, 2, RoundingMode.HALF_EVEN);
    }
}
