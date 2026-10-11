package com.loja.checkout.domain.modalidade;

import com.loja.checkout.service.ArredondamentoUtil;
import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = new BigDecimal("2.00");
        return ArredondamentoUtil.arredondar(base.add(porKg.multiply(pesoTotalKg)));
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoTotalKg) {
        return true;
    }
}
