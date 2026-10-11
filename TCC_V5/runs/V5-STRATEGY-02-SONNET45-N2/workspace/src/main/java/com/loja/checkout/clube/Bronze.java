package com.loja.checkout.clube;

import com.loja.checkout.model.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("BRONZE")
public class Bronze implements NivelClube {
    @Override
    public BigDecimal calcularCredito(Pedido pedido) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public BigDecimal ajustarFrete(BigDecimal frete) {
        return frete;
    }

    @Override
    public Boolean temBrinde(Pedido pedido) {
        return false;
    }
}
