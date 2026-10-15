package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoPix implements FormaPagamento {

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new CheckoutException(ErroCodigo.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        return totalPedido.subtract(desconto);
    }

    @Override
    public BigDecimal calcularParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }
}
