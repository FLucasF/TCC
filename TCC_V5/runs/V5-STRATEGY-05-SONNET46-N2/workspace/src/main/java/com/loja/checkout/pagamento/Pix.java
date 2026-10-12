package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Pix implements FormaPagamento {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceita(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, 1, totalFinal);
    }
}
