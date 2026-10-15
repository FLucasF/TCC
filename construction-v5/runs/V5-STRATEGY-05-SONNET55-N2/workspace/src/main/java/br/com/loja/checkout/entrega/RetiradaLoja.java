package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements Entrega {
    public String codigo() { return "RETIRADA_LOJA"; }
    public int prazoDias() { return 1; }
    public BigDecimal frete(Pedido pedido) { return Dinheiro.ZERO; }
}
