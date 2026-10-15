package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaRetiradaLoja implements OpcaoEntrega {
    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.ZERO;
    }
}
