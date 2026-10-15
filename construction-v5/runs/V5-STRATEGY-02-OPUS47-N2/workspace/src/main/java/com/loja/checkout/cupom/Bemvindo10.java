package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Bemvindo10 implements Cupom {
    public String codigo() { return "BEMVINDO10"; }
    public boolean aplicavel(Pedido p) { return true; }
    public BigDecimal desconto(Pedido p, BigDecimal frete) {
        return Dinheiro.emCentavos(p.subtotal().multiply(new BigDecimal("0.10")));
    }
}
