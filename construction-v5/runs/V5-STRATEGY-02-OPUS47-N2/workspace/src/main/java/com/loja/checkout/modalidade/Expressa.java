package com.loja.checkout.modalidade;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Expressa implements Modalidade {
    public String codigo() { return "EXPRESSA"; }
    public boolean atende(Pedido p) { return true; }
    public BigDecimal frete(Pedido p) {
        return Dinheiro.emCentavos(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(p.pesoKg())));
    }
    public int prazoDias() { return 2; }
}
