package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Nova opção de entrega = nova classe @Component implementando esta interface. */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    boolean atende(Pedido pedido);

    BigDecimal frete(Pedido pedido);
}
