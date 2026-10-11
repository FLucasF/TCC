package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.util.ArredondadorMeio;

public class FormaPagamentoBoleto implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        return new BigDecimal("3.49");
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        BigDecimal totalComTarifa = total.add(new BigDecimal("3.49"));
        return ArredondadorMeio.arredondar(totalComTarifa);
    }

    @Override
    public boolean parcelaPermitida(Integer parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean podeAtender(BigDecimal total) {
        return total.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public String nome() {
        return "BOLETO";
    }

}
