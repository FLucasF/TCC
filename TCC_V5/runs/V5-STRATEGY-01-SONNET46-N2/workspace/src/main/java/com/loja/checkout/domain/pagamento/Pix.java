package com.loja.checkout.domain.pagamento;

import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CodigoErro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public void validar(int parcelas, BigDecimal totalPedido) {
        if (parcelas != 1) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = totalPedido.multiply(DESCONTO).negate()
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = totalPedido.add(ajuste).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(totalFinal, totalFinal, ajuste, 1);
    }
}
