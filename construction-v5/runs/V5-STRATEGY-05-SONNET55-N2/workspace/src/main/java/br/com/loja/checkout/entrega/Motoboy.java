package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements Entrega {
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    public String codigo() { return "MOTOBOY"; }
    public int prazoDias() { return 0; }
    public BigDecimal frete(Pedido pedido) { return Dinheiro.reais("18.00"); }

    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
