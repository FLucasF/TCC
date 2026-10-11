package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Bronze implements NivelClube {
    public String codigo() { return "BRONZE"; }
    public BigDecimal ajustarFrete(BigDecimal frete, Pedido p) { return frete; }
    public BigDecimal credito(Pedido p) { return Dinheiro.ZERO; }
    public boolean brinde(Pedido p) { return false; }
}
