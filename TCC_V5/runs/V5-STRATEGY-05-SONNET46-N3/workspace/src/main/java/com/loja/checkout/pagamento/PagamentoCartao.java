package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoCartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final double TAXA_MENSAL = 0.0199;

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > MAX_PARCELAS) {
            throw new CheckoutException(ErroCodigo.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            return totalPedido;
        }
        BigDecimal parcela = calcularParcelaComJuros(totalPedido, parcelas);
        return parcela.multiply(BigDecimal.valueOf(parcelas));
    }

    @Override
    public BigDecimal calcularParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, int n) {
        double pow = Math.pow(1.0 + TAXA_MENSAL, -n);
        double parcela = total.doubleValue() * TAXA_MENSAL / (1.0 - pow);
        return BigDecimal.valueOf(parcela).setScale(2, RoundingMode.HALF_EVEN);
    }
}
