package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.infra.CheckoutException;

import java.math.BigDecimal;

public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal total) {
        if (total.compareTo(LIMITE) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = total.add(TARIFA);
        return new ResultadoPagamento(TARIFA, totalFinal, 1, totalFinal);
    }
}
