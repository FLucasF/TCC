package com.loja.checkout.delivery;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivelPara(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }
}
