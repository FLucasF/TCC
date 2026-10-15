package com.loja.checkout.clube;

import com.loja.checkout.model.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("OURO")
public class Ouro implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal VALOR_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(Pedido pedido) {
        return pedido.subtotalProdutos()
            .multiply(PERCENTUAL_CREDITO)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal ajustarFrete(BigDecimal frete) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public Boolean temBrinde(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(VALOR_MINIMO_BRINDE) > 0;
    }
}
