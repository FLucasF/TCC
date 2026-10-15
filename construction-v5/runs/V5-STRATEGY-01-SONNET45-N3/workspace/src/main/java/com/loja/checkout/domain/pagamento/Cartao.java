package com.loja.checkout.domain.pagamento;

import com.loja.checkout.service.ArredondamentoUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal valorParcela = calcularParcelaComJuros(totalPedido, parcelas);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        return ArredondamentoUtil.arredondar(totalFinal.subtract(totalPedido));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal ajuste, int parcelas) {
        if (parcelas <= 3) {
            return ArredondamentoUtil.arredondar(totalPedido.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
        }

        return calcularParcelaComJuros(totalPedido, parcelas);
    }

    @Override
    public boolean aceitaParcelamento(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return true;
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN));
        BigDecimal parcela = total.multiply(TAXA_JUROS).divide(denominador, 10, RoundingMode.HALF_EVEN);
        return ArredondamentoUtil.arredondar(parcela);
    }
}
