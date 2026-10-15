package com.loja.checkout.payment;

import com.loja.checkout.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixPagamento implements FormaPagamentoStrategy {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = MoneyUtils.round(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
