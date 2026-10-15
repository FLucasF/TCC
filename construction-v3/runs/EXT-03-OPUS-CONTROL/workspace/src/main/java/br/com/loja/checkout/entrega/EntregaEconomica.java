package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg do pedido, em 7 dias. */
@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = Dinheiro.de("12.00");
    private static final BigDecimal POR_KG = Dinheiro.de("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return Dinheiro.centavos(FIXO.add(POR_KG.multiply(pedido.pesoKg(), Dinheiro.PRECISAO)));
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 7;
    }
}
