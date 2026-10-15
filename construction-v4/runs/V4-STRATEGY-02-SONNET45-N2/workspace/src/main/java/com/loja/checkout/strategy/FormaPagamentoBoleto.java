package com.loja.checkout.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FormaPagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaTotal(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal ajuste, int parcelas) {
        return totalPedido.add(ajuste);
    }
}
