package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    public String codigo() { return "OURO"; }
    public BigDecimal ajustarFrete(BigDecimal frete, Pedido p) { return Dinheiro.ZERO; }
    public BigDecimal credito(Pedido p) {
        return Dinheiro.emCentavos(p.subtotal().multiply(new BigDecimal("0.05")));
    }
    public boolean brinde(Pedido p) { return p.subtotal().compareTo(LIMITE_BRINDE) > 0; }
}
