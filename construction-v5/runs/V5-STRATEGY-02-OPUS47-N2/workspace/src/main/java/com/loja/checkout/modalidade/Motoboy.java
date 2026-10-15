package com.loja.checkout.modalidade;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Motoboy implements Modalidade {
    private static final BigDecimal LIMITE_KG = new BigDecimal("5");

    public String codigo() { return "MOTOBOY"; }
    public boolean atende(Pedido p) { return p.pesoKg().compareTo(LIMITE_KG) <= 0; }
    public BigDecimal frete(Pedido p) { return Dinheiro.emCentavos(new BigDecimal("18.00")); }
    public int prazoDias() { return 0; }
}
