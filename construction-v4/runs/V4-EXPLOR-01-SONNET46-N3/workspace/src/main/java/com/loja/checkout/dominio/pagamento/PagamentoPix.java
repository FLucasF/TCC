package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.infra.CheckoutException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoPix implements FormaPagamento {

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal total) {
        // sem restrição
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ajuste = desconto.negate();
        BigDecimal totalFinal = total.add(ajuste);
        return new ResultadoPagamento(ajuste, totalFinal, 1, totalFinal);
    }
}
