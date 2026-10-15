package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaExpressa implements Entrega {
    public String codigo() { return "EXPRESSA"; }
    public int prazoDias() { return 2; }

    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pedido.pesoKg())));
    }
}
