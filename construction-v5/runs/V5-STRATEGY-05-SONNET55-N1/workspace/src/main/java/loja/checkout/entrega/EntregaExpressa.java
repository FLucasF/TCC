package loja.checkout.entrega;

import loja.checkout.Dinheiro;
import loja.checkout.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaExpressa implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(BASE.add(POR_KG.multiply(pedido.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
