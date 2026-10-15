package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Prata implements NivelClube {
    public String codigo() { return "PRATA"; }
    public BigDecimal ajustarFrete(BigDecimal frete, Pedido p) { return frete; }
    public BigDecimal credito(Pedido p) {
        return Dinheiro.emCentavos(p.subtotal().multiply(new BigDecimal("0.02")));
    }
    public boolean brinde(Pedido p) { return false; }
}
