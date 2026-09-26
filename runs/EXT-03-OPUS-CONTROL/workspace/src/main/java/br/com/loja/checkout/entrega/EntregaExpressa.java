package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 25,00 + R$ 4,50 por kg do pedido, em 2 dias. */
@Component
public class EntregaExpressa implements ModalidadeEntrega {

    private static final BigDecimal FIXO = Dinheiro.de("25.00");
    private static final BigDecimal POR_KG = Dinheiro.de("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return Dinheiro.centavos(FIXO.add(POR_KG.multiply(pedido.pesoKg(), Dinheiro.PRECISAO)));
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 2;
    }
}
