package com.loja.checkout.domain.modalidade;

import com.loja.checkout.service.ArredondamentoUtil;
import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        return ArredondamentoUtil.arredondar(base.add(porKg.multiply(pesoTotalKg)));
    }

    @Override
    public int getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoTotalKg) {
        return true;
    }
}
