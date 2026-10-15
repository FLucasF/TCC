package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.util.ArredondadorMeio;

public class FormaPagamentoPix implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = ArredondadorMeio.arredondar(total.multiply(new BigDecimal("0.05")));
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = ArredondadorMeio.arredondar(total.multiply(new BigDecimal("0.05")));
        BigDecimal totalComDesconto = total.subtract(desconto);
        return ArredondadorMeio.arredondar(totalComDesconto);
    }

    @Override
    public boolean parcelaPermitida(Integer parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean podeAtender(BigDecimal total) {
        return true;
    }

    @Override
    public String nome() {
        return "PIX";
    }

}
