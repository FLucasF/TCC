package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoCartao implements FormaPagamento {

    @Override
    public boolean isParcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido;
        }
        BigDecimal parcela = calcularParcelaComJuros(totalPedido, parcelas);
        return parcela.multiply(new BigDecimal(parcelas));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        if (parcelas <= 3) {
            return totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        }
        // totalFinal = parcela * parcelas, então parcela = totalFinal / parcelas
        return totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal totalPedido, int parcelas) {
        double taxa = 0.0199;
        double fator = Math.pow(1 + taxa, -parcelas);
        double denominador = 1.0 - fator;
        double parcelaDouble = totalPedido.doubleValue() * taxa / denominador;
        return BigDecimal.valueOf(parcelaDouble).setScale(2, RoundingMode.HALF_EVEN);
    }
}
