package com.loja.checkout.modalidade;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class RetiradaLoja implements Modalidade {
    public String codigo() { return "RETIRADA_LOJA"; }
    public boolean atende(Pedido p) { return true; }
    public BigDecimal frete(Pedido p) { return Dinheiro.ZERO; }
    public int prazoDias() { return 1; }
}
