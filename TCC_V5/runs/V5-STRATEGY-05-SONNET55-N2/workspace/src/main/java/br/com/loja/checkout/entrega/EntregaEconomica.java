package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaEconomica implements Entrega {
    public String codigo() { return "ECONOMICA"; }
    public int prazoDias() { return 7; }

    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pedido.pesoKg())));
    }
}
