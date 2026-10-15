package com.loja.checkout.domain.pagamento;

import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CodigoErro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Boleto implements FormaPagamento {

    private static final BigDecimal TAXA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public void validar(int parcelas, BigDecimal totalPedido) {
        if (parcelas != 1) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (totalPedido.compareTo(LIMITE) > 0) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TAXA).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(totalFinal, totalFinal, TAXA, 1);
    }
}
