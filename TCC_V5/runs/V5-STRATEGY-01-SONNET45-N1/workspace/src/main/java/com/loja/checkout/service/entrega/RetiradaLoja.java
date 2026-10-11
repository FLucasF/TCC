package com.loja.checkout.service.entrega;

import com.loja.checkout.domain.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class RetiradaLoja implements Modalidade {
    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean aceita(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }
}
