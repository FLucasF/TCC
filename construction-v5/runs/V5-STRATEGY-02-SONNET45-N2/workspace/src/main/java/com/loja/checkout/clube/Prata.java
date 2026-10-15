package com.loja.checkout.clube;

import com.loja.checkout.model.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("PRATA")
public class Prata implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(Pedido pedido) {
        return pedido.subtotalProdutos()
            .multiply(PERCENTUAL_CREDITO)
            .setScale(2, RoundingMode.HALF_EVEN);
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
