package com.loja.checkout.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BoletoPagamento implements FormaPagamentoStrategy {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_TOTAL_PEDIDO = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_TOTAL_PEDIDO) <= 0;
    }

    @Override
    public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
