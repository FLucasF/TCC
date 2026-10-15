package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 18,00 fixos, no mesmo dia. So leva pedidos de ate 5 kg. */
@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal CUSTO = Dinheiro.de("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = Dinheiro.de("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return Dinheiro.centavos(CUSTO);
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 0;
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
