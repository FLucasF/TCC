package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    public String codigo() { return "MENOS50"; }
    public boolean aplicavel(Pedido p) { return p.subtotal().compareTo(MINIMO) >= 0; }
    public BigDecimal desconto(Pedido p, BigDecimal frete) {
        return Dinheiro.emCentavos(new BigDecimal("50.00"));
    }
}
