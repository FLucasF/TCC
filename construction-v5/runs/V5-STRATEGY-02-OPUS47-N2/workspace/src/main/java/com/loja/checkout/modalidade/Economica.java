package com.loja.checkout.modalidade;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Economica implements Modalidade {
    public String codigo() { return "ECONOMICA"; }
    public boolean atende(Pedido p) { return true; }
    public BigDecimal frete(Pedido p) {
        return Dinheiro.emCentavos(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(p.pesoKg())));
    }
    public int prazoDias() { return 7; }
}
